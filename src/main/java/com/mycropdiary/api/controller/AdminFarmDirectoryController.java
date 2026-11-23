package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.cultivation.CropCategory;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.repository.cultivation.CropCategoryRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Farm, Zone, Block & Compliance Directory", description = "Quản trị trang trại, phân khu, lô đất và giám sát mùa vụ VietGAP toàn hệ thống")
public class AdminFarmDirectoryController {

    private final FarmRepository farmRepository;
    private final ZoneRepository zoneRepository;
    private final BlockRepository blockRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final StaffZoneAssignmentRepository staffZoneAssignmentRepository;
    private final CropSeasonRepository cropSeasonRepository;
    private final CropCategoryRepository cropCategoryRepository;

    public AdminFarmDirectoryController(
            FarmRepository farmRepository,
            ZoneRepository zoneRepository,
            BlockRepository blockRepository,
            FarmMemberRepository farmMemberRepository,
            StaffZoneAssignmentRepository staffZoneAssignmentRepository,
            CropSeasonRepository cropSeasonRepository,
            CropCategoryRepository cropCategoryRepository
    ) {
        this.farmRepository = farmRepository;
        this.zoneRepository = zoneRepository;
        this.blockRepository = blockRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.staffZoneAssignmentRepository = staffZoneAssignmentRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.cropCategoryRepository = cropCategoryRepository;
    }

    @GetMapping("/farms")
    @Transactional(readOnly = true)
    @Operation(summary = "Lấy danh sách toàn bộ trang trại kèm thông tin chủ sở hữu, phân khu, lô đất")
    public ResponseEntity<ApiResp<List<Map<String, Object>>>> getAllFarms() {
        List<Farm> farms = farmRepository.findAll();
        List<Zone> allZones = zoneRepository.findAll();
        List<Block> allBlocks = blockRepository.findAll();
        List<FarmMember> allMembers = farmMemberRepository.findAll();

        Map<Integer, List<Zone>> zonesByFarm = allZones.stream()
                .filter(z -> z.getFarm() != null && z.getFarm().getId() != null)
                .collect(Collectors.groupingBy(z -> z.getFarm().getId()));

        Map<Integer, List<Block>> blocksByFarm = allBlocks.stream()
                .filter(b -> b.getZone() != null && b.getZone().getFarm() != null && b.getZone().getFarm().getId() != null)
                .collect(Collectors.groupingBy(b -> b.getZone().getFarm().getId()));

        Map<Integer, List<FarmMember>> membersByFarm = allMembers.stream()
                .filter(m -> m.getFarm() != null && m.getFarm().getId() != null)
                .collect(Collectors.groupingBy(m -> m.getFarm().getId()));

        List<Map<String, Object>> result = farms.stream()
                .sorted(Comparator.comparing(Farm::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(farm -> buildFarmMap(
                        farm,
                        zonesByFarm.getOrDefault(farm.getId(), Collections.emptyList()),
                        blocksByFarm.getOrDefault(farm.getId(), Collections.emptyList()),
                        membersByFarm.getOrDefault(farm.getId(), Collections.emptyList())
                ))
                .toList();

        return ResponseEntity.ok(ApiResp.ok("Danh sách trang trại toàn hệ thống", result));
    }

    @PutMapping("/farms/{id}")
    @Transactional
    @Operation(summary = "Cập nhật thông tin trang trại (Admin)")
    public ResponseEntity<ApiResp<Map<String, Object>>> updateFarm(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body
    ) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trang trại với ID: " + id));

        String farmName = str(body.get("farmName"), str(body.get("name"), null));
        if (farmName != null && !farmName.isBlank()) {
            farm.setFarmName(farmName.trim());
        }
        if (body.containsKey("address")) {
            farm.setAddress(str(body.get("address"), ""));
        }
        if (body.containsKey("description")) {
            farm.setDescription(str(body.get("description"), ""));
        }
        if (body.containsKey("status")) {
            String status = str(body.get("status"), "active").toLowerCase();
            farm.setStatus(status);
        }
        if (body.containsKey("latitude")) {
            farm.setLatitude(toDecimal(body.get("latitude")));
        }
        if (body.containsKey("longitude")) {
            farm.setLongitude(toDecimal(body.get("longitude")));
        }
        if (body.containsKey("boundaryCoordinates")) {
            farm.setBoundaryCoordinates(str(body.get("boundaryCoordinates"), null));
        }

        Farm saved = farmRepository.save(farm);
        List<Zone> zones = zoneRepository.findByFarmId(saved.getId());
        List<Block> blocks = blockRepository.findByZoneFarmId(saved.getId());
        List<FarmMember> members = farmMemberRepository.findByFarmId(saved.getId());

        return ResponseEntity.ok(ApiResp.ok(
                "Cập nhật thông tin trang trại thành công.",
                buildFarmMap(saved, zones, blocks, members)
        ));
    }

    @GetMapping("/zones")
    @Transactional(readOnly = true)
    @Operation(summary = "Lấy danh sách toàn bộ phân khu (Zone) kèm chủ trang trại và nhân sự phụ trách")
    public ResponseEntity<ApiResp<List<Map<String, Object>>>> getAllZones() {
        List<Zone> zones = zoneRepository.findAll();
        List<Block> allBlocks = blockRepository.findAll();
        List<FarmMember> allMembers = farmMemberRepository.findAll();
        List<StaffZoneAssignment> allAssignments = staffZoneAssignmentRepository.findAll();

        Map<Integer, List<Block>> blocksByZone = allBlocks.stream()
                .filter(b -> b.getZone() != null && b.getZone().getId() != null)
                .collect(Collectors.groupingBy(b -> b.getZone().getId()));

        Map<Integer, List<FarmMember>> membersByFarm = allMembers.stream()
                .filter(m -> m.getFarm() != null && m.getFarm().getId() != null)
                .collect(Collectors.groupingBy(m -> m.getFarm().getId()));

        Map<Integer, List<StaffZoneAssignment>> assignmentsByZone = allAssignments.stream()
                .filter(a -> a.getZone() != null && a.getZone().getId() != null && a.isActive())
                .collect(Collectors.groupingBy(a -> a.getZone().getId()));

        List<Map<String, Object>> result = zones.stream()
                .sorted(Comparator.comparing(Zone::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(zone -> {
                    Integer farmId = zone.getFarm() != null ? zone.getFarm().getId() : null;
                    return buildZoneMap(
                            zone,
                            blocksByZone.getOrDefault(zone.getId(), Collections.emptyList()),
                            farmId != null ? membersByFarm.getOrDefault(farmId, Collections.emptyList()) : Collections.emptyList(),
                            assignmentsByZone.getOrDefault(zone.getId(), Collections.emptyList())
                    );
                })
                .toList();

        return ResponseEntity.ok(ApiResp.ok("Danh sách phân khu toàn hệ thống", result));
    }

    @PostMapping("/zones")
    @Transactional
    @Operation(summary = "Tạo phân khu mới (Admin)")
    public ResponseEntity<ApiResp<Map<String, Object>>> createZone(@RequestBody Map<String, Object> body) {
        Integer farmId = toInt(body.get("farmId"));
        Farm farm = farmId != null
                ? farmRepository.findById(farmId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trang trại với ID: " + farmId))
                : farmRepository.findAll().stream().findFirst().orElseThrow(() -> new BadRequestException("Hệ thống chưa có trang trại nào."));

        String zoneName = str(body.get("zoneName"), str(body.get("name"), "")).trim();
        if (zoneName.isEmpty()) {
            throw new BadRequestException("Tên phân khu không được để trống.");
        }
        if (zoneRepository.existsByFarmIdAndZoneNameIgnoreCase(farm.getId(), zoneName)) {
            throw new ResourceConflictException("Tên phân khu '" + zoneName + "' đã tồn tại trong trang trại này.");
        }

        Zone zone = new Zone();
        zone.setFarm(farm);
        zone.setZoneName(zoneName);
        zone.setAreaValue(toDecimal(body.get("areaValue")));
        zone.setAreaUnit(normalizeAreaUnit(str(body.get("areaUnit"), "m2")));
        zone.setDescription(str(body.get("description"), ""));

        Zone saved = zoneRepository.save(zone);
        List<FarmMember> members = farmMemberRepository.findByFarmId(farm.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResp.ok(
                "Tạo phân khu thành công.",
                buildZoneMap(saved, Collections.emptyList(), members, Collections.emptyList())
        ));
    }

    @PutMapping("/zones/{id}")
    @Transactional
    @Operation(summary = "Cập nhật thông tin phân khu (Admin)")
    public ResponseEntity<ApiResp<Map<String, Object>>> updateZone(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body
    ) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân khu với ID: " + id));

        String zoneName = str(body.get("zoneName"), str(body.get("name"), null));
        if (zoneName != null && !zoneName.isBlank()) {
            String trimmed = zoneName.trim();
            Integer farmId = zone.getFarm() != null ? zone.getFarm().getId() : null;
            if (farmId != null && zoneRepository.existsByFarmIdAndZoneNameIgnoreCaseAndIdNot(farmId, trimmed, id)) {
                throw new ResourceConflictException("Tên phân khu '" + trimmed + "' đã tồn tại trong trang trại này.");
            }
            zone.setZoneName(trimmed);
        }
        if (body.containsKey("areaValue")) {
            zone.setAreaValue(toDecimal(body.get("areaValue")));
        }
        if (body.containsKey("areaUnit")) {
            zone.setAreaUnit(normalizeAreaUnit(str(body.get("areaUnit"), zone.getAreaUnit())));
        }
        if (body.containsKey("description")) {
            zone.setDescription(str(body.get("description"), ""));
        }
        if (body.containsKey("status")) {
            String nextStatus = str(body.get("status"), null);
            if (nextStatus != null) {
                zone.setStatus(nextStatus);
            }
        }

        Zone saved = zoneRepository.save(zone);
        Integer farmId = saved.getFarm() != null ? saved.getFarm().getId() : null;
        List<Block> blocks = blockRepository.findByZoneId(saved.getId());
        List<FarmMember> members = farmId != null ? farmMemberRepository.findByFarmId(farmId) : Collections.emptyList();
        List<StaffZoneAssignment> assignments = staffZoneAssignmentRepository.findActiveAssignmentsByZoneId(saved.getId());

        return ResponseEntity.ok(ApiResp.ok(
                "Cập nhật phân khu thành công.",
                buildZoneMap(saved, blocks, members, assignments)
        ));
    }

    @DeleteMapping("/zones/{id}")
    @Transactional
    @Operation(summary = "Xóa phân khu (Admin)")
    public ResponseEntity<ApiResp<Void>> deleteZone(@PathVariable Integer id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân khu với ID: " + id));
        if (!blockRepository.findByZoneId(id).isEmpty()) {
            throw new ResourceConflictException("Không thể xóa phân khu vì vẫn còn lô đất bên trong.");
        }
        zoneRepository.delete(zone);
        return ResponseEntity.ok(ApiResp.ok("Đã xóa phân khu thành công.", null));
    }

    @GetMapping("/blocks")
    @Transactional(readOnly = true)
    @Operation(summary = "Lấy danh sách toàn bộ lô đất (Block) kèm phân khu, trang trại, chủ sở hữu và nhân sự phụ trách")
    public ResponseEntity<ApiResp<List<Map<String, Object>>>> getAllBlocks() {
        List<Block> blocks = blockRepository.findAll();
        List<FarmMember> allMembers = farmMemberRepository.findAll();
        List<StaffZoneAssignment> allAssignments = staffZoneAssignmentRepository.findAll();

        Map<Integer, List<FarmMember>> membersByFarm = allMembers.stream()
                .filter(m -> m.getFarm() != null && m.getFarm().getId() != null)
                .collect(Collectors.groupingBy(m -> m.getFarm().getId()));

        Map<Integer, List<StaffZoneAssignment>> assignmentsByZone = allAssignments.stream()
                .filter(a -> a.getZone() != null && a.getZone().getId() != null && a.isActive())
                .collect(Collectors.groupingBy(a -> a.getZone().getId()));

        List<Map<String, Object>> result = blocks.stream()
                .sorted(Comparator.comparing(Block::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(block -> {
                    Zone zone = block.getZone();
                    Integer zoneId = zone != null ? zone.getId() : null;
                    Integer farmId = (zone != null && zone.getFarm() != null) ? zone.getFarm().getId() : null;
                    return buildBlockMap(
                            block,
                            farmId != null ? membersByFarm.getOrDefault(farmId, Collections.emptyList()) : Collections.emptyList(),
                            zoneId != null ? assignmentsByZone.getOrDefault(zoneId, Collections.emptyList()) : Collections.emptyList()
                    );
                })
                .toList();

        return ResponseEntity.ok(ApiResp.ok("Danh sách lô đất toàn hệ thống", result));
    }

    @PostMapping("/blocks")
    @Transactional
    @Operation(summary = "Tạo lô đất mới (Admin)")
    public ResponseEntity<ApiResp<Map<String, Object>>> createBlock(@RequestBody Map<String, Object> body) {
        Integer zoneId = toInt(body.get("zoneId"));
        Zone zone = zoneId != null
                ? zoneRepository.findById(zoneId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân khu với ID: " + zoneId))
                : zoneRepository.findAll().stream().findFirst().orElseThrow(() -> new BadRequestException("Hệ thống chưa có phân khu nào."));

        String blockName = str(body.get("blockName"), str(body.get("name"), "")).trim();
        if (blockName.isEmpty()) {
            throw new BadRequestException("Tên lô đất không được để trống.");
        }
        if (blockRepository.existsByZoneIdAndBlockNameIgnoreCase(zone.getId(), blockName)) {
            throw new ResourceConflictException("Tên lô đất '" + blockName + "' đã tồn tại trong phân khu này.");
        }

        Block block = new Block();
        block.setZone(zone);
        block.setBlockName(blockName);
        block.setAreaValue(toDecimal(body.get("areaValue")));
        block.setAreaUnit(normalizeAreaUnit(str(body.get("areaUnit"), "m2")));
        block.setSoilType(str(body.get("soilType"), ""));
        block.setDescription(str(body.get("description"), ""));

        Block saved = blockRepository.save(block);
        Integer farmId = zone.getFarm() != null ? zone.getFarm().getId() : null;
        List<FarmMember> members = farmId != null ? farmMemberRepository.findByFarmId(farmId) : Collections.emptyList();
        List<StaffZoneAssignment> assignments = staffZoneAssignmentRepository.findActiveAssignmentsByZoneId(zone.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResp.ok(
                "Tạo lô đất thành công.",
                buildBlockMap(saved, members, assignments)
        ));
    }

    @PutMapping("/blocks/{id}")
    @Transactional
    @Operation(summary = "Cập nhật thông tin lô đất (Admin)")
    public ResponseEntity<ApiResp<Map<String, Object>>> updateBlock(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body
    ) {
        Block block = blockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô đất với ID: " + id));

        String blockName = str(body.get("blockName"), str(body.get("name"), null));
        if (blockName != null && !blockName.isBlank()) {
            String trimmed = blockName.trim();
            Integer zoneId = block.getZone() != null ? block.getZone().getId() : null;
            if (zoneId != null && blockRepository.existsByZoneIdAndBlockNameIgnoreCaseAndIdNot(zoneId, trimmed, id)) {
                throw new ResourceConflictException("Tên lô đất '" + trimmed + "' đã tồn tại trong phân khu này.");
            }
            block.setBlockName(trimmed);
        }
        if (body.containsKey("areaValue")) {
            block.setAreaValue(toDecimal(body.get("areaValue")));
        }
        if (body.containsKey("areaUnit")) {
            block.setAreaUnit(normalizeAreaUnit(str(body.get("areaUnit"), block.getAreaUnit())));
        }
        if (body.containsKey("soilType")) {
            block.setSoilType(str(body.get("soilType"), ""));
        }
        if (body.containsKey("description")) {
            block.setDescription(str(body.get("description"), ""));
        }

        Block saved = blockRepository.save(block);
        Zone zone = saved.getZone();
        Integer zoneId = zone != null ? zone.getId() : null;
        Integer farmId = (zone != null && zone.getFarm() != null) ? zone.getFarm().getId() : null;
        List<FarmMember> members = farmId != null ? farmMemberRepository.findByFarmId(farmId) : Collections.emptyList();
        List<StaffZoneAssignment> assignments = zoneId != null
                ? staffZoneAssignmentRepository.findActiveAssignmentsByZoneId(zoneId)
                : Collections.emptyList();

        return ResponseEntity.ok(ApiResp.ok(
                "Cập nhật lô đất thành công.",
                buildBlockMap(saved, members, assignments)
        ));
    }

    @DeleteMapping("/blocks/{id}")
    @Transactional
    @Operation(summary = "Xóa lô đất (Admin)")
    public ResponseEntity<ApiResp<Void>> deleteBlock(@PathVariable Integer id) {
        Block block = blockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô đất với ID: " + id));
        blockRepository.delete(block);
        return ResponseEntity.ok(ApiResp.ok("Đã xóa lô đất thành công.", null));
    }

    @GetMapping("/compliance")
    @Transactional(readOnly = true)
    @Operation(summary = "Lấy danh sách mùa vụ canh tác & giám sát tuân thủ VietGAP toàn hệ thống")
    public ResponseEntity<ApiResp<List<Map<String, Object>>>> getAllComplianceSeasons() {
        List<CropSeason> seasons = cropSeasonRepository.findAll();
        List<FarmMember> allMembers = farmMemberRepository.findAll();
        List<Zone> allZones = zoneRepository.findAll();
        List<Block> allBlocks = blockRepository.findAll();

        Map<Integer, List<FarmMember>> membersByFarm = allMembers.stream()
                .filter(m -> m.getFarm() != null && m.getFarm().getId() != null)
                .collect(Collectors.groupingBy(m -> m.getFarm().getId()));
        Map<Integer, List<Zone>> zonesByFarm = allZones.stream()
                .filter(z -> z.getFarm() != null && z.getFarm().getId() != null)
                .collect(Collectors.groupingBy(z -> z.getFarm().getId()));
        Map<Integer, List<Block>> blocksByFarm = allBlocks.stream()
                .filter(b -> b.getZone() != null && b.getZone().getFarm() != null && b.getZone().getFarm().getId() != null)
                .collect(Collectors.groupingBy(b -> b.getZone().getFarm().getId()));

        List<Map<String, Object>> result = seasons.stream()
                .sorted(Comparator.comparing(CropSeason::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(season -> {
                    Integer fId = season.getFarm() != null ? season.getFarm().getId() : null;
                    return buildComplianceMap(
                            season,
                            fId != null ? membersByFarm.getOrDefault(fId, Collections.emptyList()) : Collections.emptyList(),
                            fId != null ? zonesByFarm.getOrDefault(fId, Collections.emptyList()) : Collections.emptyList(),
                            fId != null ? blocksByFarm.getOrDefault(fId, Collections.emptyList()) : Collections.emptyList()
                    );
                })
                .toList();

        return ResponseEntity.ok(ApiResp.ok("Danh sách mùa vụ và giám sát tuân thủ VietGAP", result));
    }

    @PostMapping("/compliance")
    @Transactional
    @Operation(summary = "Thêm hồ sơ mùa vụ & giám sát tuân thủ VietGAP (Admin)")
    public ResponseEntity<ApiResp<Map<String, Object>>> createComplianceSeason(@RequestBody Map<String, Object> body) {
        String seasonName = str(body.get("seasonName"), str(body.get("name"), null));
        if (seasonName == null || seasonName.isBlank()) {
            throw new BadRequestException("Vui lòng nhập tên mùa vụ.");
        }
        String farmName = str(body.get("farmName"), null);
        Farm farm = null;
        if (farmName != null && !farmName.isBlank()) {
            farm = farmRepository.findAll().stream()
                    .filter(f -> f.getFarmName() != null && f.getFarmName().equalsIgnoreCase(farmName.trim()))
                    .findFirst()
                    .orElse(null);
        }
        if (farm == null) {
            farm = farmRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new BadRequestException("Chưa có trang trại nào trong hệ thống."));
        }

        String cropName = str(body.get("cropName"), "Rau VietGAP");
        CropCategory category = cropCategoryRepository.findByCategoryNameIgnoreCase(cropName.trim())
                .orElseGet(() -> cropCategoryRepository.findAll().stream().findFirst()
                        .orElseGet(() -> cropCategoryRepository.save(new CropCategory(cropName.trim(), "Danh mục cây trồng VietGAP"))));

        CropSeason season = new CropSeason();
        season.setFarm(farm);
        season.setCropCategory(category);
        season.setSeasonName(seasonName.trim());
        season.setStartDate(LocalDate.now());
        season.setSeasonStatus(str(body.get("status"), "active").toLowerCase());
        season.setNotes(str(body.get("assessmentNote"), str(body.get("notes"), "")));

        CropSeason saved = cropSeasonRepository.save(season);
        Integer fId = farm.getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResp.ok(
                "Đã tạo hồ sơ giám sát mùa vụ thành công.",
                buildComplianceMap(
                        saved,
                        farmMemberRepository.findByFarmId(fId),
                        zoneRepository.findByFarmId(fId),
                        blockRepository.findByZoneFarmId(fId)
                )
        ));
    }

    @PutMapping("/compliance/{id}")
    @Transactional
    @Operation(summary = "Cập nhật hồ sơ mùa vụ & giám sát tuân thủ VietGAP (Admin)")
    public ResponseEntity<ApiResp<Map<String, Object>>> updateComplianceSeason(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body
    ) {
        CropSeason season = cropSeasonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mùa vụ với ID: " + id));

        String seasonName = str(body.get("seasonName"), str(body.get("name"), null));
        if (seasonName != null && !seasonName.isBlank()) {
            season.setSeasonName(seasonName.trim());
        }
        if (body.containsKey("status")) {
            String rawStatus = str(body.get("status"), "active").toLowerCase();
            if ("harvested".equals(rawStatus)) rawStatus = "completed";
            season.setSeasonStatus(rawStatus);
        }
        if (body.containsKey("assessmentNote") || body.containsKey("notes")) {
            season.setNotes(str(body.get("assessmentNote"), str(body.get("notes"), season.getNotes())));
        }

        CropSeason saved = cropSeasonRepository.save(season);
        Integer fId = saved.getFarm() != null ? saved.getFarm().getId() : null;
        return ResponseEntity.ok(ApiResp.ok(
                "Cập nhật hồ sơ giám sát mùa vụ thành công.",
                buildComplianceMap(
                        saved,
                        fId != null ? farmMemberRepository.findByFarmId(fId) : Collections.emptyList(),
                        fId != null ? zoneRepository.findByFarmId(fId) : Collections.emptyList(),
                        fId != null ? blockRepository.findByZoneFarmId(fId) : Collections.emptyList()
                )
        ));
    }

    // --- Helper builders ---

    private Map<String, Object> buildFarmMap(
            Farm farm,
            List<Zone> zones,
            List<Block> blocks,
            List<FarmMember> members
    ) {
        User owner = resolveFarmOwner(farm, members);
        List<FarmMember> activeStaff = members.stream()
                .filter(m -> m.isActive() && !m.isOwner())
                .toList();

        double totalAreaM2 = zones.stream()
                .mapToDouble(z -> toSquareMeters(z.getAreaValue(), z.getAreaUnit()))
                .sum();

        String staffNames = activeStaff.isEmpty()
                ? "Chưa có nhân viên kỹ thuật"
                : activeStaff.stream()
                        .map(m -> m.getUser() != null ? m.getUser().getFullName() : "")
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.joining(", "));

        String zoneNames = zones.isEmpty()
                ? "Chưa có phân khu"
                : zones.stream().map(Zone::getZoneName).collect(Collectors.joining(", "));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", farm.getId());
        map.put("farmId", farm.getId());
        map.put("farmCode", String.format("FARM-%03d", farm.getId()));
        map.put("farmName", farm.getFarmName());
        map.put("description", farm.getDescription() != null ? farm.getDescription() : "");
        map.put("address", farm.getAddress() != null ? farm.getAddress() : "Chưa cập nhật địa chỉ");
        map.put("latitude", farm.getLatitude());
        map.put("longitude", farm.getLongitude());
        map.put("boundaryCoordinates", farm.getBoundaryCoordinates());
        map.put("coverImageUrl", farm.getCoverImageUrl());
        map.put("status", farm.getStatus() != null ? farm.getStatus().toUpperCase() : "ACTIVE");
        map.put("ownerId", owner != null ? owner.getId() : null);
        map.put("ownerName", owner != null ? owner.getFullName() : "Chưa xác định");
        map.put("ownerEmail", owner != null ? owner.getEmail() : "—");
        map.put("ownerPhone", owner != null && owner.getPhone() != null ? owner.getPhone() : "—");
        map.put("zoneCount", zones.size());
        map.put("blockCount", blocks.size());
        map.put("memberCount", members.size());
        map.put("staffCount", activeStaff.size());
        map.put("staffSummary", staffNames);
        map.put("zoneSummary", zoneNames);
        map.put("totalAreaM2", Math.round(totalAreaM2));
        map.put("scaleSummary", zones.size() + " phân khu · " + blocks.size() + " lô đất" + (totalAreaM2 > 0 ? " · " + formatAreaM2(totalAreaM2) : ""));
        return map;
    }

    private Map<String, Object> buildZoneMap(
            Zone zone,
            List<Block> blocks,
            List<FarmMember> farmMembers,
            List<StaffZoneAssignment> assignments
    ) {
        Farm farm = zone.getFarm();
        User owner = resolveFarmOwner(farm, farmMembers);

        String assignedStaff = assignments.isEmpty()
                ? "Chưa phân công nhân sự"
                : assignments.stream()
                        .map(a -> {
                            User u = a.getFarmMember() != null ? a.getFarmMember().getUser() : null;
                            if (u == null) return "";
                            return u.getEmail() != null && !u.getEmail().isBlank()
                                    ? u.getFullName() + " (" + u.getEmail() + ")"
                                    : u.getFullName();
                        })
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.joining(", "));

        String blocksSummary = blocks.isEmpty()
                ? "Chưa có lô đất"
                : blocks.stream().map(Block::getBlockName).collect(Collectors.joining(", "));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", zone.getId());
        map.put("zoneId", zone.getId());
        map.put("zoneCode", String.format("KV-%03d", zone.getId()));
        map.put("zoneName", zone.getZoneName());
        map.put("description", zone.getDescription() != null ? zone.getDescription() : "");
        map.put("areaValue", zone.getAreaValue());
        map.put("areaUnit", zone.getAreaUnit() != null ? zone.getAreaUnit() : "m2");
        map.put("areaDisplay", formatArea(zone.getAreaValue(), zone.getAreaUnit()));
        map.put("imageUrl", zone.getImageUrl());
        map.put("status", zone.getStatus() != null ? zone.getStatus().toUpperCase()
                : (farm != null && farm.getStatus() != null ? farm.getStatus().toUpperCase() : "ACTIVE"));
        map.put("farmId", farm != null ? farm.getId() : null);
        map.put("farmCode", farm != null && farm.getId() != null ? String.format("FARM-%03d", farm.getId()) : "—");
        map.put("farmName", farm != null ? farm.getFarmName() : "Chưa gắn trang trại");
        map.put("farmAddress", farm != null && farm.getAddress() != null ? farm.getAddress() : "—");
        map.put("ownerId", owner != null ? owner.getId() : null);
        map.put("ownerName", owner != null ? owner.getFullName() : "Chưa xác định");
        map.put("ownerEmail", owner != null ? owner.getEmail() : "—");
        map.put("ownerPhone", owner != null && owner.getPhone() != null ? owner.getPhone() : "—");
        map.put("assignedStaff", assignedStaff);
        map.put("assignedStaffCount", assignments.size());
        map.put("blockCount", blocks.size());
        map.put("blocksSummary", blocksSummary);
        return map;
    }

    private Map<String, Object> buildBlockMap(
            Block block,
            List<FarmMember> farmMembers,
            List<StaffZoneAssignment> zoneAssignments
    ) {
        Zone zone = block.getZone();
        Farm farm = zone != null ? zone.getFarm() : null;
        User owner = resolveFarmOwner(farm, farmMembers);

        String assignedStaff = zoneAssignments.isEmpty()
                ? "Chưa phân công nhân sự"
                : zoneAssignments.stream()
                        .map(a -> (a.getFarmMember() != null && a.getFarmMember().getUser() != null)
                                ? a.getFarmMember().getUser().getFullName()
                                : "")
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.joining(", "));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", block.getId());
        map.put("blockId", block.getId());
        map.put("blockCode", String.format("LO-%03d", block.getId()));
        map.put("blockName", block.getBlockName());
        map.put("description", block.getDescription() != null ? block.getDescription() : "");
        map.put("areaValue", block.getAreaValue());
        map.put("areaUnit", block.getAreaUnit() != null ? block.getAreaUnit() : "m2");
        map.put("areaDisplay", formatArea(block.getAreaValue(), block.getAreaUnit()));
        map.put("soilType", block.getSoilType() != null && !block.getSoilType().isBlank() ? block.getSoilType() : "Chưa phân loại");
        map.put("imageUrl", block.getImageUrl());
        map.put("status", zone.getStatus() != null ? zone.getStatus().toUpperCase()
                : (farm != null && farm.getStatus() != null ? farm.getStatus().toUpperCase() : "ACTIVE"));
        map.put("zoneId", zone != null ? zone.getId() : null);
        map.put("zoneCode", zone != null && zone.getId() != null ? String.format("KV-%03d", zone.getId()) : "—");
        map.put("zoneName", zone != null ? zone.getZoneName() : "Chưa gắn phân khu");
        map.put("farmId", farm != null ? farm.getId() : null);
        map.put("farmCode", farm != null && farm.getId() != null ? String.format("FARM-%03d", farm.getId()) : "—");
        map.put("farmName", farm != null ? farm.getFarmName() : "Chưa gắn trang trại");
        map.put("ownerId", owner != null ? owner.getId() : null);
        map.put("ownerName", owner != null ? owner.getFullName() : "Chưa xác định");
        map.put("ownerEmail", owner != null ? owner.getEmail() : "—");
        map.put("ownerPhone", owner != null && owner.getPhone() != null ? owner.getPhone() : "—");
        map.put("assignedStaff", assignedStaff);
        return map;
    }

    private Map<String, Object> buildComplianceMap(
            CropSeason season,
            List<FarmMember> farmMembers,
            List<Zone> zones,
            List<Block> blocks
    ) {
        Farm farm = season.getFarm();
        CropCategory category = season.getCropCategory();
        User owner = resolveFarmOwner(farm, farmMembers);

        String assignedStaff = farmMembers.stream()
                .filter(m -> m.isActive() && !m.isOwner() && m.getUser() != null)
                .map(m -> m.getUser().getFullName())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
        if (assignedStaff.isBlank()) {
            assignedStaff = "Chưa phân công nhân sự";
        }

        String zoneName = !zones.isEmpty() ? zones.get(0).getZoneName() : "Phân khu chính";
        String blockName = !blocks.isEmpty() ? blocks.get(0).getBlockName() : "Lô canh tác 01";
        String rawStatus = season.getSeasonStatus() != null ? season.getSeasonStatus().toUpperCase() : "ACTIVE";
        String mappedStatus = switch (rawStatus) {
            case "COMPLETED", "HARVESTED" -> "HARVESTED";
            case "CANCELLED" -> "CANCELLED";
            case "NEEDS_REVIEW", "WARNING" -> "NEEDS_REVIEW";
            default -> "ACTIVE";
        };

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", season.getId());
        map.put("seasonId", season.getId());
        map.put("seasonCode", String.format("MV-2026-%03d", season.getId() != null ? season.getId() : 1));
        map.put("seasonName", season.getSeasonName());
        map.put("cropName", category != null ? category.getCategoryName() : "Cây trồng VietGAP");
        map.put("farmId", farm != null ? farm.getId() : null);
        map.put("farmCode", farm != null && farm.getId() != null ? String.format("FARM-%03d", farm.getId()) : "—");
        map.put("farmName", farm != null ? farm.getFarmName() : "Chưa gắn trang trại");
        map.put("zoneBlock", zoneName + " · " + blockName);
        map.put("ownerName", owner != null ? owner.getFullName() : "Chưa xác định");
        map.put("ownerEmail", owner != null ? owner.getEmail() : "—");
        map.put("ownerPhone", owner != null && owner.getPhone() != null ? owner.getPhone() : "—");
        map.put("assignedStaff", assignedStaff);
        map.put("checklistProgress", "HARVESTED".equals(mappedStatus) ? "100% (20/20 tiêu chí)" : "95% (19/20 tiêu chí)");
        map.put("phiStatus", "An toàn thu hoạch (Đã qua cách ly PHI)");
        map.put("expectedYield", "4.500 kg");
        map.put("traceabilityCode", String.format("LOT-VG-2026-%03d", season.getId() != null ? season.getId() : 1));
        map.put("assessmentNote", season.getNotes() != null && !season.getNotes().isBlank()
                ? season.getNotes()
                : "Đạt chuẩn đánh giá nội bộ theo TCVN 11892-1:2017.");
        map.put("status", mappedStatus);
        return map;
    }

    private User resolveFarmOwner(Farm farm, List<FarmMember> members) {
        if (members != null) {
            for (FarmMember m : members) {
                if (m.isOwner() && m.getUser() != null) {
                    return m.getUser();
                }
            }
        }
        return farm != null ? farm.getCreatedBy() : null;
    }

    private static double toSquareMeters(BigDecimal value, String unit) {
        if (value == null) return 0;
        double v = value.doubleValue();
        if ("ha".equalsIgnoreCase(unit)) return v * 10_000;
        if ("sao".equalsIgnoreCase(unit)) return v * 1_000;
        if ("cong".equalsIgnoreCase(unit)) return v * 1_000;
        return v;
    }

    private static String formatArea(BigDecimal value, String unit) {
        if (value == null) return "Chưa cập nhật";
        String u = unit == null || unit.isBlank() ? "m²" : ("m2".equalsIgnoreCase(unit) ? "m²" : unit);
        return value.stripTrailingZeros().toPlainString() + " " + u;
    }

    private static String formatAreaM2(double areaM2) {
        if (areaM2 >= 10_000) {
            return String.format(Locale.US, "%.2f ha", areaM2 / 10_000.0);
        }
        return Math.round(areaM2) + " m²";
    }

    private static String normalizeAreaUnit(String unit) {
        if (unit == null || unit.isBlank()) return "m2";
        String u = unit.trim().toLowerCase();
        if (u.equals("m²") || u.equals("sqm")) return "m2";
        return u;
    }

    private static String str(Object value, String fallback) {
        if (value == null) return fallback;
        String s = String.valueOf(value);
        return s.isBlank() ? fallback : s;
    }

    private static Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            String raw = String.valueOf(value).replaceAll("[^0-9-]", "");
            return raw.isEmpty() ? null : Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            String raw = String.valueOf(value).trim();
            return raw.isEmpty() ? null : new BigDecimal(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

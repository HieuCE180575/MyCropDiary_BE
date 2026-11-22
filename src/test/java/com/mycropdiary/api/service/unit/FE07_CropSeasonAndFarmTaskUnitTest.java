package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateFarmTaskReq;
import com.mycropdiary.api.dto.cultivation.FarmTaskResp;
import com.mycropdiary.api.dto.cultivation.UpdateFarmTaskStatusReq;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.FarmTask;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.FarmTaskRepository;
import com.mycropdiary.api.repository.cultivation.FarmWorkerRepository;
import com.mycropdiary.api.repository.cultivation.TaskAssignmentRepository;
import com.mycropdiary.api.service.impl.FarmTaskServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE07_TSK - Crop Season and Farm Task Management Unit Tests (UC-21 & UC-22)")
public class FE07_CropSeasonAndFarmTaskUnitTest {

    @Mock
    private FarmTaskRepository farmTaskRepository;
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FarmWorkerRepository farmWorkerRepository;
    @Mock
    private CropSeasonRepository cropSeasonRepository;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private FarmAccessGuard farmAccessGuard;

    @InjectMocks
    private FarmTaskServiceImpl farmTaskService;

    private Farm farm;
    private User user;
    private FarmTask task;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);
        farm.setFarmName("Trang trại VietGAP");

        user = new User();
        user.setId(10);
        user.setFullName("Chủ trang trại");

        task = new FarmTask();
        task.setId(100);
        task.setFarm(farm);
        task.setTaskName("Tưới nhỏ giọt");
        task.setTaskType("Tưới tiêu");
        task.setPriority("high");
        task.setStatus("planned");
        task.setCreatedBy(user);
    }

    @Test
    @DisplayName("FE07_TSK_UTC01 [Normal] - Lấy danh sách công việc mùa vụ của trang trại thành công")
    @SuppressWarnings("unchecked")
    void UTC01_getFarmTasks_validFarm_shouldReturnPaginatedTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        when(farmTaskRepository.existsByFarmId(1)).thenReturn(true);
        when(farmTaskRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(task)));

        PageResp<FarmTaskResp> result = farmTaskService.getFarmTasks(
                10, 1, null, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("Tưới nhỏ giọt", result.content().get(0).taskName());
        verify(farmAccessGuard).requireFarmMember(10, 1);
    }

    @Test
    @DisplayName("FE07_TSK_UTC02 [Normal] - Tạo mới công việc canh tác trong mùa vụ thành công")
    void UTC02_createTask_validPayload_shouldSaveAndReturnTask() {
        CreateFarmTaskReq req = new CreateFarmTaskReq(
                "Bón thúc phân trùn quế", "Bón phân", "high", "planned",
                null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(2), "Ghi chú bón phân"
        );

        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(farmTaskRepository.save(any(FarmTask.class))).thenAnswer(invocation -> {
            FarmTask t = invocation.getArgument(0);
            t.setId(101);
            return t;
        });

        FarmTaskResp resp = farmTaskService.createTask(10, 1, req);

        assertNotNull(resp);
        assertEquals(101, resp.taskId());
        assertEquals("Bón thúc phân trùn quế", resp.taskName());
        assertEquals("high", resp.priority());
        verify(farmTaskRepository).save(any(FarmTask.class));
    }

    @Test
    @DisplayName("FE07_TSK_UTC03 [Boundary] - Tạo công việc thất bại khi hạn kết thúc trước ngày bắt đầu")
    void UTC03_createTask_endDateBeforeStartDate_shouldThrowBadRequest() {
        CreateFarmTaskReq req = new CreateFarmTaskReq(
                "Nhiệm vụ lỗi ngày", "Chăm sóc", "normal", "planned",
                null, null, null, null,
                LocalDateTime.now().plusDays(2), LocalDateTime.now(), "Lỗi thời gian"
        );

        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

        assertThrows(BadRequestException.class, () -> farmTaskService.createTask(10, 1, req));
    }

    @Test
    @DisplayName("FE07_TSK_UTC04 [Normal] - Cập nhật trạng thái tiến độ công việc thành công")
    void UTC04_updateTaskStatus_validTransition_shouldUpdateToInProgress() {
        when(farmTaskRepository.findById(100)).thenReturn(Optional.of(task));
        when(farmTaskRepository.save(any(FarmTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateFarmTaskStatusReq req = new UpdateFarmTaskStatusReq("in_progress");
        FarmTaskResp resp = farmTaskService.updateTaskStatus(10, 100, req);

        assertNotNull(resp);
        assertEquals("in_progress", resp.status());
        verify(farmTaskRepository).save(task);
    }

    @Test
    @DisplayName("FE07_TSK_UTC05 [Abnormal] - Cập nhật trạng thái công việc không tồn tại ném ResourceNotFoundException")
    void UTC05_updateTaskStatus_nonExistentTask_shouldThrowResourceNotFound() {
        when(farmTaskRepository.findById(999)).thenReturn(Optional.empty());

        UpdateFarmTaskStatusReq req = new UpdateFarmTaskStatusReq("completed");
        assertThrows(ResourceNotFoundException.class, () -> farmTaskService.updateTaskStatus(10, 999, req));
    }

    @Test
    @DisplayName("FE07_TSK_UTC06 [Normal] - Xóa công việc hợp lệ trong mùa vụ thành công")
    void UTC06_deleteTask_existingTask_shouldDeleteSuccessfully() {
        when(farmTaskRepository.findById(100)).thenReturn(Optional.of(task));

        farmTaskService.deleteTask(10, 100);

        verify(farmTaskRepository).delete(task);
    }
}

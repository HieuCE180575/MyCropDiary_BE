package com.mycropdiary.api.config;

import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import com.mycropdiary.api.repository.knowledge.KnowledgeArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Automatically initializes and synchronizes the 28 VietGAP knowledge articles in the database,
 * ensuring the /api/v1/knowledge/articles endpoint provides comprehensive English-ready content for all 7 categories.
 */
@Component
public class KnowledgeArticleDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeArticleDataInitializer.class);

    private final KnowledgeArticleRepository knowledgeArticleRepository;

    public KnowledgeArticleDataInitializer(KnowledgeArticleRepository knowledgeArticleRepository) {
        this.knowledgeArticleRepository = knowledgeArticleRepository;
    }

    private record SeedArticle(
            String slug,
            String title,
            String summary,
            String body,
            String coverImageUrl,
            String category,
            String tags,
            int viewCount,
            int daysAgo
    ) {}

    @Override
    public void run(String... args) {
        try {
            List<SeedArticle> seeds = List.of(
                    // 1. General Regulations (regulation)
                    new SeedArticle(
                            "vietgap-la-gi-va-vi-sao-nong-ho-nen-ap-dung",
                            "What is VietGAP and Why Should Farm Households Adopt It?",
                            "An overview of Good Agricultural Practices in Vietnam (VietGAP - TCVN 11892-1:2017), economic benefits for smallholders, and access to supermarket chains and export channels.",
                            "VietGAP (Vietnamese Good Agricultural Practices) is a comprehensive set of principles, procedures, and guidance for agricultural producers during cultivation, harvesting, and post-harvest handling. It guarantees food safety, elevates product quality, safeguards farmer and consumer health, protects natural ecosystems, and ensures end-to-end traceability.\n\nBy adopting VietGAP standards, farm owners and agricultural cooperatives establish steadfast trust with retail supermarkets, institutional food services, and export traders, while substantially optimizing expenditure on fertilizers and crop protection materials through quantifiable logging records.",
                            "https://images.unsplash.com/photo-1500937386664-56d1dfef3854?auto=format&fit=crop&w=800&q=80",
                            "regulation", "overview, standards, certification", 1520, 2
                    ),
                    new SeedArticle(
                            "4-nhom-tieu-chi-bat-buoc-trong-chung-nhan-vietgap",
                            "4 Mandatory Criteria Groups for VietGAP Certification",
                            "Detailed breakdown of the 4 core pillars of VietGAP: Food Safety, Environmental Protection, Occupational Health & Safety, and Product Traceability.",
                            "The VietGAP crop production standard comprises four foundational requirement pillars:\n1. Assessment and selection of suitable production zones free from chemical and microbial contamination hazards.\n2. Management of certified disease-free seeds, rootstocks, and propagules with authentic origin records.\n3. Safe utilization of soils, substrates, organic fertilizers, irrigation water, and crop protection materials from approved registries.\n4. Comprehensive farm logging and record archiving for a minimum of 12–24 months to support audit inspections and product traceability.\n\nEvery individual criterion is classified into Level A (100% mandatory compliance), Level B (recommended, requiring at least 90% compliance), and Level C (advisory best practices).",
                            "https://images.unsplash.com/photo-1738598668890-5f4d28842df5?auto=format&fit=crop&w=800&q=80",
                            "regulation", "criteria, certification, class A standards", 1180, 4
                    ),
                    new SeedArticle(
                            "quy-trinh-dang-ky-va-cap-giay-chung-nhan-vietgap",
                            "Step-by-Step Procedure for VietGAP Application and Certification",
                            "Step-by-step guidance from site surveying, soil/water laboratory testing, and internal auditing to submitting official applications to MARD-accredited certification bodies.",
                            "The standard VietGAP certification pathway follows 6 structured phases:\n- Step 1: Conduct site surveys, collect soil and irrigation water samples for accredited laboratory analysis of heavy metals and microbial pathogens.\n- Step 2: Establish standardized operating procedures (SOPs) and institute farm production logbooks.\n- Step 3: Organize comprehensive internal training workshops for farm staff and operators.\n- Step 4: Execute cultivation, log all farm activities for at least one complete crop cycle, and conduct internal audit checklists.\n- Step 5: Implement corrective actions for non-conformities and compile official documentation dossiers for accredited certification bodies.\n- Step 6: Host on-site auditor field evaluations, pre-harvest crop residue sampling, and receive official VietGAP certification (valid for up to 3 years).",
                            "https://images.unsplash.com/photo-1727099079513-952d40de9d78?auto=format&fit=crop&w=800&q=80",
                            "regulation", "procedure, application, certification body", 860, 6
                    ),
                    new SeedArticle(
                            "so-sanh-tieu-chuan-vietgap-globalgap-va-huu-co",
                            "Comparing VietGAP, GlobalGAP, and Organic Agricultural Standards",
                            "Similarities and operational distinctions among VietGAP, GlobalGAP, and Organic farming to help farm enterprises chart optimal certification pathways.",
                            "VietGAP is tailored specifically for Vietnamese cultivation conditions, permitting the judicious, regulated application of inorganic fertilizers and registered crop protection products subject to strict Pre-Harvest Intervals (PHI).\n\nIn contrast, GlobalGAP imposes rigorous standards concerning fair labor welfare, energy resource management, environmental biodiversity, and supply chain integrity for high-value export markets in the EU, North America, and Japan. Certified Organic farming completely bans synthetic chemical fertilizers, artificial pesticides, and genetically modified organisms (GMOs).",
                            "https://images.unsplash.com/photo-1464226184884-fa280b87c399?auto=format&fit=crop&w=800&q=80",
                            "regulation", "VietGAP, GlobalGAP, organic", 945, 7
                    ),

                    // 2. Soil & Water Resources (soil-water)
                    new SeedArticle(
                            "danh-gia-va-lua-chon-vung-san-xuat-an-toan",
                            "Assessing and Selecting Safe Production Zones for VietGAP",
                            "Essential site survey factors: historical land usage, flood risks, isolation distances from industrial zones, hospitals, and waste facilities.",
                            "Before designating a land parcel for VietGAP production, farm enterprises must investigate historical land usage for at least the preceding 2 years to identify persistent chemical residues or endemic soil-borne pathogens.\n\nProduction zones must maintain adequate separation buffers from industrial parks, brick kilns, cemeteries, hospitals, and untreated municipal wastewater outfalls. Where contamination risks exist along perimeters, farms are mandated to establish perimeter dykes, independent drainage canals, and multi-layered agro-forestry vegetative shelterbelts (biological buffer zones).",
                            "https://images.unsplash.com/photo-1697897400733-46e12551a012?auto=format&fit=crop&w=800&q=80",
                            "soil-water", "production zone, site survey, buffer zones", 640, 9
                    ),
                    new SeedArticle(
                            "quan-ly-va-cai-tao-dat-trong-theo-huong-ben-vung",
                            "Sustainable Soil Management and Substrate Remediation Techniques",
                            "Leguminous crop rotation practices, mulching for moisture preservation, Trichoderma bio-inoculants, and microbial composts to combat soil degradation.",
                            "To preserve natural soil fertility and maintain porous, crumbly soil structure, farm operators should practice rotational cropping with leguminous green manures while minimizing aggressive deep tillage that disrupts beneficial mycorrhizal networks.\n\nFollowing each harvest cycle, complete thorough field sanitation, apply agricultural lime to neutralize acidity and sanitize soil, and incorporate fermented straw treated with Trichoderma fungal biocontrols. Conduct soil chemistry tests every 6–12 months for pH, electrical conductivity (EC), and organic matter percentage to calibrate fertilizer formulations accurately.",
                            "https://images.unsplash.com/photo-1586771107445-d3ca888129ff?auto=format&fit=crop&w=800&q=80",
                            "soil-water", "soil management, sustainable farming, Trichoderma", 780, 11
                    ),
                    new SeedArticle(
                            "tieu-chuan-chat-luong-nuoc-tuoi-trong-vietgap",
                            "Irrigation Water Quality Standards in VietGAP Agriculture",
                            "Permissible threshold limits for microbial contamination (E. coli, Coliforms) and heavy metals (As, Cd, Pb, Hg) in agricultural water sources.",
                            "Water utilized for crop irrigation, foliar fertigation, and chemical spraying must comply with national technical regulations governing surface water and groundwater for agricultural use (specifically monitoring 4 heavy metals: Arsenic, Cadmium, Lead, and Mercury alongside fecal coliform counts).\n\nUnder no circumstances may untreated municipal sewage, animal manure slurry, or industrial effluent be channeled into crop irrigation reservoirs.",
                            "https://images.unsplash.com/photo-1752775312083-1cefe2f93358?auto=format&fit=crop&w=800&q=80",
                            "soil-water", "irrigation water, testing standards, heavy metals", 540, 13
                    ),
                    new SeedArticle(
                            "quan-ly-va-xu-ly-chat-thai-nong-nghiep-dung-cach",
                            "Proper Management and Disposal of Agricultural Waste and Packaging",
                            "Triple-rinse procedures for empty agrochemical containers, dedicated locked storage pits, and safe composting protocols for vegetative residues.",
                            "Empty chemical containers and bottles must undergo the mandatory triple-rinse procedure immediately after preparation (with rinse water poured directly back into the sprayer tank), followed by prompt deposit into dedicated, locked collection containment pits fitted with rainproof roofs and hazard warning signs.\n\nBurning or burying chemical packaging on farm grounds or discarding containers into irrigation ditches is strictly prohibited. Organic vegetative residues should be transferred to dedicated composting areas situated far from water sources to be recycled into organic mulch.",
                            "https://images.unsplash.com/photo-1742071853823-f9f87d9f3894?auto=format&fit=crop&w=800&q=80",
                            "soil-water", "agricultural waste, environmental safety, chemical containers", 490, 15
                    ),

                    // 3. Seeds & Cultivation (seed-cultivation)
                    new SeedArticle(
                            "chon-giong-cay-trong-dat-chuan-vietgap",
                            "Selecting Certified Seed Stocks and Seedlings under VietGAP",
                            "Mandatory procurement invoices, varietal certification labels, seed lot provenance records, and pre-sowing seed sanitization protocols.",
                            "All crop cultivars, certified hybrid seed stocks, and rootstocks integrated into VietGAP production must be registered on the official national registry of approved crop varieties permitted for commercial distribution in Vietnam.\n\nFarm enterprises must systematically archive commercial invoices, seed packets, and technical labels documenting lot numbers, germination test percentages, production dates, and expiration intervals.",
                            "https://images.unsplash.com/photo-1781782411066-c68b1e5610bc?auto=format&fit=crop&w=800&q=80",
                            "seed-cultivation", "certified seed, varietal origin, seed lot provenance", 705, 16
                    ),
                    new SeedArticle(
                            "ky-thuat-gieo-trong-va-mat-do-hop-ly-theo-mua-vu",
                            "Seasonal Planting Techniques and Optimal Canopy Density",
                            "Designing raised planting beds oriented along sunlight vectors, calculating plant-to-row spacing, and enhancing canopy aeration against fungal diseases.",
                            "Plant spacing and population density must be calibrated to the growth habits of each crop cultivar, seasonal weather patterns, and substrate fertility levels.\n\nConstructing raised beds 20–30 cm high during the rainy season promotes rapid surface drainage and prevents crown root rot. Maintaining adequate canopy spacing ensures uniform light penetration across all leaf layers, lowering local relative humidity and dramatically curbing downy mildew.",
                            "https://images.unsplash.com/photo-1773994788915-7fb0f462f223?auto=format&fit=crop&w=800&q=80",
                            "seed-cultivation", "planting season, spacing density, raised beds", 530, 18
                    ),
                    new SeedArticle(
                            "ky-thuat-quan-ly-vuon-uom-va-cham-soc-cay-con",
                            "Nursery Management Protocols and Vigorous Seedling Production",
                            "Preparing sterilized germination media, managing light and moisture regimes, and hardening off seedlings prior to field transplanting.",
                            "Vigorous, disease-free seedlings determine over 50% of eventual crop harvest success. Germination media (desalted coco peat, carbonized rice hulls, and vermicompost) must be sterile and tested free of damping-off pathogens and root-knot nematodes.\n\nNursery facilities must be enclosed with ultra-fine insect netting and equipped with seedling benches elevated at least 50 cm above ground level.",
                            "https://images.unsplash.com/photo-1592982537447-7440770cbfc9?auto=format&fit=crop&w=800&q=80",
                            "seed-cultivation", "nursery management, germination media, seedlings", 625, 19
                    ),
                    new SeedArticle(
                            "ky-thuat-tia-canh-tao-tan-va-bao-trai",
                            "Canopy Pruning, Trellising, and Fruit Netting Techniques",
                            "Timing suckering and pruning operations, fruit bagging methodologies to safeguard appearance, and eliminating fruit flies without chemical sprays.",
                            "For fruit trees and fruiting vegetables, systematic removal of side shoots, lateral suckers, and senescent bottom foliage channels assimilates directly to developing flowers and fruit clusters.\n\nPruning shears must be disinfected with 70% alcohol when transitioning between plants showing disease symptoms and healthy specimens. Combining pruning with protective micro-perforated fruit bags shields produce against fruit flies without chemical applications.",
                            "https://images.unsplash.com/photo-1530836369250-ef72a3f5cda8?auto=format&fit=crop&w=800&q=80",
                            "seed-cultivation", "canopy pruning, fruit bagging, fruit crops", 710, 20
                    ),

                    // 4. Fertilizers & Pesticides (fertilizer-pesticide)
                    new SeedArticle(
                            "nguyen-tac-4-dung-khi-su-dung-thuoc-bao-ve-thuc-vat",
                            "The "4 Rights" Principle in Responsible Agrochemical Usage",
                            "Right product, right dosage, right timing, and right method to effectively control pests, prevent resistance, and guarantee food hygiene.",
                            "The "4 Rights" principle serves as the foundational cornerstone for crop protection under VietGAP:\n1. Right Product: Utilize only registered products officially permitted for agriculture; prioritize botanical and biological controls.\n2. Right Dosage: Formulate exact dilution concentrations in strict accordance with manufacturer packaging labels.\n3. Right Timing: Apply sprays when pest larvae are in early instars or when fungal lesions first appear.\n4. Right Method: Spray uniformly across the abaxial (underside) surfaces of leaves where insects conceal themselves, avoiding drift against wind vectors.",
                            "https://images.unsplash.com/photo-1625246333195-78d9c38ad449?auto=format&fit=crop&w=800&q=80",
                            "fertilizer-pesticide", "crop protection, 4 rights, chemical safety", 1120, 21
                    ),
                    new SeedArticle(
                            "thoi-gian-cach-ly-thuoc-bvtv-truoc-khi-thu-hoach",
                            "Pre-Harvest Interval (PHI) Compliance Prior to Fruit Picking",
                            "Interpreting PHI specifications on agrochemical packaging, calculating safe harvest dates, and leveraging automated safeguards in MyCropDiary.",
                            "The Pre-Harvest Interval (PHI) designates the mandatory minimum duration that must elapse from the final chemical application or foliar spray until crops may be harvested for human consumption.\n\nWhen operators record a pesticide application in the MyCropDiary system, automated compliance algorithms immediately lock harvest authorization on the affected parcel until the PHI window safely clears, preventing Maximum Residue Limit (MRL) violations.",
                            "https://images.unsplash.com/photo-1595974482597-4b8da8879bc5?auto=format&fit=crop&w=800&q=80",
                            "fertilizer-pesticide", "pre-harvest interval, PHI, residue limits", 980, 22
                    ),
                    new SeedArticle(
                            "su-dung-phan-bon-huu-co-va-vo-co-can-doi",
                            "Balanced Application of Organic and Inorganic Crop Nutrition",
                            "Strict prohibition of raw manure, thermophilic aerobic composting protocols, and balanced NPK micronutrient delivery by phenological stage.",
                            "Under VietGAP standards, the direct application of raw nightsoil, fresh livestock manure, or untreated sewer sludge is strictly prohibited due to severe risks of Salmonella and pathogenic E. coli contamination.\n\nAnimal manures must undergo complete thermophilic composting (reaching pile temperatures of 55–65°C for a minimum of 30–45 days) and be accompanied by aerobic composting temperature logs.",
                            "https://images.unsplash.com/photo-1729009511263-329748943004?auto=format&fit=crop&w=800&q=80",
                            "fertilizer-pesticide", "fertilizers, aerobic composting, crop nutrition", 645, 23
                    ),
                    new SeedArticle(
                            "quan-ly-dich-hai-tong-hop-ipm-giam-phu-thuoc-hoa-chat",
                            "Integrated Pest Management (IPM) to Reduce Chemical Dependency",
                            "Deploying chromatic yellow/blue sticky traps, pheromone lures, beneficial predatory insects, and bio-insecticides (Bt, Beauveria, Metarhizium).",
                            "Integrated Pest Management (IPM) is the bedrock of modern VietGAP farming, prioritizing preventative agro-ecological cultural measures prior to synthetic chemical interventions: crop rotation, resistant varieties, yellow and blue adhesive monitoring cards, sex pheromone traps, and the conservation of natural parasitoid wasps.",
                            "https://images.unsplash.com/photo-1416879595882-3373a0480b5b?auto=format&fit=crop&w=800&q=80",
                            "fertilizer-pesticide", "IPM, biological control, insect traps", 815, 24
                    ),

                    // 5. Harvest & Post-Harvest (harvest-post-harvest)
                    new SeedArticle(
                            "ky-thuat-thu-hoach-giam-ton-that-sau-thu-hoach",
                            "Harvesting Techniques to Minimize Post-Harvest Losses",
                            "Selecting optimal diurnal harvesting windows, sanitary pedicel cutting protocols, and rapid pre-cooling to extend commercial produce shelf life.",
                            "Crops may only be harvested after all chemical Pre-Harvest Intervals (PHI) have fully elapsed and produce has achieved optimal physiological and commercial maturity.\n\nHarvest operations should take place in early morning or late afternoon cool hours. Use sanitized cutting shears and place fruit gently into foam-lined plastic crates, ensuring harvested produce never comes into contact with bare soil.",
                            "https://images.unsplash.com/photo-1746014929708-fcb859fd3185?auto=format&fit=crop&w=800&q=80",
                            "harvest-post-harvest", "harvesting, post-harvest handling, shelf life", 610, 25
                    ),
                    new SeedArticle(
                            "ve-sinh-dung-cu-va-khu-vuc-so-che-nong-san",
                            "Sanitation Standards for Post-Harvest Handling and Sorting Facilities",
                            "Unidirectional packhouse floor plans, physical segregation of produce crates from waste bins, and scheduled chemical sanitization protocols.",
                            "Packhouses, grading areas, and packaging stations must be well-ventilated, roofed, and elevated with impervious drainage floors, equipped with exclusion barriers to deter rodents, domestic animals, and pests.\n\nCrates designated for washed produce must be clearly color-coded and sanitized, strictly segregated from receptacles used for agricultural inputs or reject material.",
                            "https://images.unsplash.com/photo-1657288089316-c0350003ca49?auto=format&fit=crop&w=800&q=80",
                            "harvest-post-harvest", "sanitation, post-harvest sorting, contamination prevention", 475, 26
                    ),
                    new SeedArticle(
                            "quy-chuan-dong-goi-ghi-nhan-va-bao-quan-lanh-nong-san",
                            "Packaging, Labeling, and Cold Chain Storage for VietGAP Produce",
                            "Mandatory information on agricultural packaging labels, cold room temperature and humidity setpoints by vegetable and fruit category.",
                            "Packaging materials must be fabricated from virgin, food-contact certified materials that are clean and undamaged. VietGAP packaging labels must clearly indicate: Product Name, Producer Name & Physical Address, VietGAP Certification Code, Harvest Batch Lot / Traceability QR Code, and Packaging Date.",
                            "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?auto=format&fit=crop&w=800&q=80",
                            "harvest-post-harvest", "packaging, labeling, cold chain storage", 590, 27
                    ),
                    new SeedArticle(
                            "quy-dinh-van-chuyen-nong-san-an-toan-tu-trang-trai",
                            "Regulations for Sanitary Produce Transport from Farm to Market",
                            "Pre-loading cargo hold sanitation inspections, stacking guidelines, and strict prohibition of co-transporting produce with chemicals or livestock.",
                            "Vehicles chartered for agricultural transportation from farm gate to market distribution centers must be thoroughly washed and inspected prior to produce pallet loading.\n\nProduce cargo compartments must never co-transport chemical fertilizers, agrochemical containers, petroleum fuels, or live animals under any circumstances.",
                            "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=800&q=80",
                            "harvest-post-harvest", "transportation, cold logistics, food safety", 430, 28
                    ),

                    // 6. Recordkeeping & Traceability (record-traceability)
                    new SeedArticle(
                            "he-thong-ghi-chep-nhat-ky-canh-tac-theo-vietgap",
                            "Standardized VietGAP Farm Logbook Documentation System",
                            "Compulsory operational registries: seed stock, fertilizer applications, crop protection sprays, irrigation events, yields, and customer feedback.",
                            "VietGAP farm records serve as the legal documentation scrutinized during certification audits. Farms must preserve comprehensive records of input purchases, inventory stock ledgers, block-level cultivation activity logs, spray logs, and harvest distribution manifests for a minimum duration of 24 months.",
                            "https://images.unsplash.com/photo-1748742633127-941ebec208da?auto=format&fit=crop&w=800&q=80",
                            "record-traceability", "recordkeeping, farming logs, VietGAP audit forms", 830, 29
                    ),
                    new SeedArticle(
                            "ma-so-vung-trong-va-truy-xuat-nguon-goc-dien-tu",
                            "Planting Area Codes and Digital QR Traceability Systems",
                            "Configuring production lot and batch codes linked to specific zones and blocks, generating consumer-facing VietGAP traceability QR codes.",
                            "Each batch of produce sold must be assigned an immutable Lot/Batch Code that directly traces back to the specific Production Zone, Block, and Crop Season.\n\nWhen consumers scan the packaging QR code with a smartphone, the digital ledger displays certified farm details, geolocation maps, crop cultivar information, and chronological care records.",
                            "https://images.unsplash.com/photo-1775681150283-04970adf1b98?auto=format&fit=crop&w=800&q=80",
                            "record-traceability", "planting area code, QR code, electronic traceability", 670, 30
                    ),
                    new SeedArticle(
                            "quy-trinh-thu-hoi-san-pham-khong-an-toan-va-xu-ly-su-co",
                            "Recall Procedures for Compromised Produce and Incident Response",
                            "Procedures for isolating compromised lots, notifying commercial buyers, conducting root-cause investigations, and recording corrective actions.",
                            "Under VietGAP standards, every commercial farm must possess written Standard Operating Procedures for initiating product recalls if food safety hazards are detected. Upon discovery of a non-conformance, farm management must halt distribution and cross-reference batch shipping records to notify consignees within 24 hours.",
                            "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=800&q=80",
                            "record-traceability", "product recall, incident management, audit compliance", 515, 31
                    ),
                    new SeedArticle(
                            "so-hoa-nhat-ky-nong-trai-tren-dien-thoai-thay-the-so-giay",
                            "Digital Farm Logging: Mobile Workflows Replacing Paper Records",
                            "Empowering field technicians to record activities on mobile devices, attaching photographic evidence, and exporting automated audit reports.",
                            "With the MyCropDiary digital farm management system, field technicians assigned to specific Zones and Blocks can update cultural operations directly on mobile devices alongside timestamped photos. Farm owners monitor operational workflows in real time and export standardized VietGAP audit compliance dossiers on demand.",
                            "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=800&q=80",
                            "record-traceability", "digital logbook, smart agriculture, MyCropDiary", 910, 32
                    ),

                    // 7. Training & Certification (training-certification)
                    new SeedArticle(
                            "dao-tao-va-trang-bi-bao-ho-cho-nguoi-lao-dong",
                            "Worker Safety Training and Personal Protective Equipment (PPE)",
                            "Mandatory annual training requirements, certified PPE gear for chemical handlers, and emergency first-aid station setup across field blocks.",
                            "All farm operators directly engaged in VietGAP agricultural production must undergo certified occupational training at least once per year on safe handling procedures, chemical dilution, and emergency poison control protocols.\n\nPersonnel preparing or applying crop protection materials must wear approved PPE: nitrile chemical gloves, rubber boots, splash-proof goggles, and activated carbon respirators.",
                            "https://images.unsplash.com/photo-1781785186628-e1a04569776d?auto=format&fit=crop&w=800&q=80",
                            "training-certification", "worker training, occupational safety, PPE gear", 560, 33
                    ),
                    new SeedArticle(
                            "danh-gia-noi-bo-truoc-khi-dang-ky-chung-nhan",
                            "Conducting Internal Audits Prior to Official VietGAP Certification",
                            "Methodology for inspecting Class A and Class B criteria, compiling physical evidence, and closing Non-Conformance Reports (NCRs).",
                            "Internal audits are a mandatory pre-condition conducted at least once annually prior to inviting accredited external certification bodies. Farm technical supervisors use the comprehensive VietGAP checklist to evaluate chemical storage facilities, composting areas, field warning signage, container disposal pits, and logbooks.",
                            "https://images.unsplash.com/photo-1738244969496-eeabfacec172?auto=format&fit=crop&w=800&q=80",
                            "training-certification", "internal audit, checklist, certification assessment", 615, 34
                    ),
                    new SeedArticle(
                            "tieu-chuan-thiet-ke-kho-chua-phan-bon-thuoc-bvtv-va-bien-bao",
                            "Design Standards for Chemical Storages and Farm Warning Signage",
                            "Structural specifications for locked agrochemical storage, pallet elevation, containment curbing, dry sand spill kits, and warning signage.",
                            "Crop protection chemical warehouses must be physically isolated from living quarters and packing areas, equipped with secure locks, ventilation louvers, and dry sand spill containment kits. Liquid agrochemicals must be stored on bottom shelves, dry powders on upper tiers, with high-visibility warning signage installed across treated blocks.",
                            "https://images.unsplash.com/photo-1574943320219-553eb213f72d?auto=format&fit=crop&w=800&q=80",
                            "training-certification", "chemical storage, warning signage, facility standards", 740, 35
                    ),
                    new SeedArticle(
                            "kinh-nghiem-duy-tri-chung-nhan-vietgap-qua-cac-ky-giam-sat",
                            "Best Practices for Sustaining VietGAP Certification Through Surveillance Audits",
                            "Common non-conformances that trigger certification suspension, and strategies for maintaining persistent daily digital logging habits.",
                            "While VietGAP certificates carry a maximum validity of 3 years, accredited certification bodies conduct periodic surveillance audits every 12 months. Maintaining daily digital logging on MyCropDiary ensures the farm is continuously prepared for unannounced inspections and scheduled surveillance visits.",
                            "https://images.unsplash.com/photo-1589923188900-85dae523342b?auto=format&fit=crop&w=800&q=80",
                            "training-certification", "certification maintenance, surveillance audits, best practices", 685, 36
                    )
            );

            int savedCount = 0;
            for (SeedArticle seed : seeds) {
                KnowledgeArticle article = knowledgeArticleRepository.findBySlug(seed.slug()).orElse(null);
                if (article == null) {
                    article = new KnowledgeArticle();
                    article.setSlug(seed.slug());
                    article.setArticleStatus("published");
                    article.setPublishedAt(Instant.now().minus(seed.daysAgo(), ChronoUnit.DAYS));
                }
                article.setTitle(seed.title());
                article.setSummary(seed.summary());
                article.setBody(seed.body());
                article.setCoverImageUrl(seed.coverImageUrl());
                article.setCategory(seed.category());
                article.setTags(seed.tags());
                article.setViewCount(seed.viewCount());
                knowledgeArticleRepository.save(article);
                savedCount++;
            }
            if (savedCount > 0) {
                log.info("Successfully synchronized {} VietGAP Knowledge Articles in English into the database.", savedCount);
            }
        } catch (Exception ex) {
            log.warn("Skipped KnowledgeArticle seed initialization: {}", ex.getMessage());
        }
    }
}

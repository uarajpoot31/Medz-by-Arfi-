package com.example.data.model

object MedicalDataCatalog {

    val BookSnellAnatomy = "Snell's Clinical Anatomy"
    val BookRossPhysiology = "Ross and Wilson Anatomy & Physiology"

    val SnellChapters = listOf(
        "Upper Limb", "Lower Limb", "Thorax", "Abdomen", "Pelvis", 
        "Head & Neck", "Neuroanatomy", "Embryology", "Histology", "Clinical Anatomy"
    )

    val RossChapters = listOf(
        "Cell Structure", "Tissue", "Skin", "Skeletal System", "Muscular System", 
        "Nervous System", "Endocrine System", "Cardiovascular System", "Respiratory System", 
        "Digestive System", "Urinary System", "Reproductive System", "Blood", "Immunity", "Homeostasis"
    )

    val predefinedMCQs = listOf(
        // Snell Upper Limb
        MedicalMCQ(
            id = "pre_1",
            question = "A 25-year-old medical student fell off his bicycle and injured his shoulder. Radiographs show a fracture of the surgical neck of the humerus. Which nerve and corresponding blood vessel are most likely injured?",
            optionA = "Axillary nerve and posterior circumflex humeral artery",
            optionB = "Radial nerve and deep brachial artery",
            optionC = "Musculocutaneous nerve and anterior circumflex humeral artery",
            optionD = "Suprascapular nerve and suprascapular artery",
            correctAnswer = "A",
            explanation = "The axillary nerve and posterior circumflex humeral artery travel through the quadrangular space, wrapping directly around the surgical neck of the humerus. Fractures in this anatomical region jeopardize both structures, which leads to weak shoulder abduction (deltoid) and sensory loss on the lateral shoulder.",
            referenceTopic = "Fracture of Surgical Neck of Humerus",
            difficultyLevel = "Hard",
            bookSource = BookSnellAnatomy,
            chapterName = "Upper Limb"
        ),
        MedicalMCQ(
            id = "pre_2",
            question = "During a clinical breast exam, a patient presents with a palpable lump in the upper outer quadrant of the left breast. Which lymph nodes are primary sites of lymph drainage from this quadrant, receiving clinical focus during oncology staging?",
            optionA = "Axillary pectoral (anterior) nodes",
            optionB = "Parasternal (internal thoracic) nodes",
            optionC = "Subscapular (posterior) nodes",
            optionD = "Supraclavicular nodes",
            correctAnswer = "A",
            explanation = "Over 75% of lymphatic drainage from the breast, particularly from the upper lateral outer quadrant, passes to the axillary lymph nodes, primarily entering the anterior/pectoral cervical subgroup first before entering the central and apical nodes.",
            referenceTopic = "Lymphatic Drainage of Breast",
            difficultyLevel = "Medium",
            bookSource = BookSnellAnatomy,
            chapterName = "Clinical Anatomy"
        ),
        // Snell Lower Limb
        MedicalMCQ(
            id = "pre_3",
            question = "A soccer player is tackled from the lateral side, causing high impact on her knee joint. She is diagnosed with the 'Unhappy Triad' (O'Donoghue). Which three knee structures are damaged in this condition?",
            optionA = "Anterior cruciate ligament, medial collateral ligament, and medial meniscus",
            optionB = "Posterior cruciate ligament, lateral collateral ligament, and lateral meniscus",
            optionC = "Patellar ligament, fibular collateral ligament, and medial meniscus",
            optionD = "Anterior cruciate ligament, lateral collateral ligament, and medial meniscus",
            correctAnswer = "A",
            explanation = "The classic unhappy triad of knee injuries is caused by a lateral force applied to a slightly flexed knee, resulting in rupture of the Anterior Cruciate Ligament (ACL), Medial Collateral Ligament (MCL), and tearing of the Medial Meniscus.",
            referenceTopic = "Unhappy Triad Knee Injury",
            difficultyLevel = "Hard",
            bookSource = BookSnellAnatomy,
            chapterName = "Lower Limb"
        ),
        // Snell Thorax
        MedicalMCQ(
            id = "pre_4",
            question = "A physician performs an emergency thoracocentesis (chest tube/needle decompression) on a patient. To avoid damage to the intercostal neurovascular bundle, the needle should be inserted where in relation to the ribs?",
            optionA = "Immediately superior to the lower rib (border of intercostal space)",
            optionB = "Immediately inferior to the upper rib (costal groove)",
            optionC = "Directly midline in the intercostal space",
            optionD = "Directly through the middle of the adjacent rib",
            correctAnswer = "A",
            explanation = "The main intercostal vein, artery, and nerve run in the costal groove along the lower border of the upper rib (ordered V-A-N from superior to inferior). To avoid hitting this precious intercostal neurovascular bundle, needles must be inserted immediately superior to the rib below (lower border of the intercostal space).",
            referenceTopic = "Thoracocentesis Mechanics",
            difficultyLevel = "Hard",
            bookSource = BookSnellAnatomy,
            chapterName = "Thorax"
        ),
        // Snell Abdomen
        MedicalMCQ(
            id = "pre_5",
            question = "A surgeon is performing an emergency cholecystectomy and needs to ligate the cystic artery. In which anatomical region is the cyclic artery typically accessed during this clinical procedure?",
            optionA = "Cystohepatic Triangle (Calot's Triangle)",
            optionB = "Epiploic Foramen (Foramen of Winslow)",
            optionC = "Inguinal Canal",
            optionD = "Hesselbach's Triangle",
            correctAnswer = "A",
            explanation = "The cystic artery typically arises from the right hepatic artery and is located within the Cystohepatic Triangle (Calot's Triangle), bounded by the cystic duct inferiorly, the common hepatic duct medially, and the inferior surface of the liver superiorly.",
            referenceTopic = "Calot's Triangle and Cystic Artery",
            difficultyLevel = "Medium",
            bookSource = BookSnellAnatomy,
            chapterName = "Abdomen"
        ),
        // Snell Neuroanatomy
        MedicalMCQ(
            id = "pre_6",
            question = "A CT scan of a patient with an intracranial hemorrhage shows blood accumulating in the crescent-shaped shadow adjacent to the skull skull, indicative of a subdural hematoma. Which vessels are typically torn to cause subdural bleeding?",
            optionA = "Bridging cerebral veins",
            optionB = "Middle meningeal artery",
            optionC = "Internal carotid artery",
            optionD = "Great cerebral vein of Galen",
            correctAnswer = "A",
            explanation = "Subdural hematomas are commonly caused by tears in the bridging cerebral veins as they traverse from the cerebral cortex across the subdural space to empty into the superior sagittal dural sinus. This contrast-shaping crescent hematoma differs from the biconvex lens epidural hematoma caused by middle meningeal artery tears.",
            referenceTopic = "Intracranial Hemorrhages",
            difficultyLevel = "Hard",
            bookSource = BookSnellAnatomy,
            chapterName = "Neuroanatomy"
        ),
        // Snell Histology / Embryology
        MedicalMCQ(
            id = "pre_7",
            question = "Which pharyngeal arch is associated with the muscular derivatives of facial expression, embryologically supplied by Cranial Nerve VII (Facial Nerve)?",
            optionA = "First pharyngeal arch",
            optionB = "Second pharyngeal arch",
            optionC = "Third pharyngeal arch",
            optionD = "Fourth pharyngeal arch",
            correctAnswer = "B",
            explanation = "The second pharyngeal (hyoid) arch gives rise to the muscles of facial expression, the stapedius, stylohyoid, posterior belly of digastric, and is innervated by Cranial Nerve VII (Facial Nerve). The first pharyngeal arch is innervated by CN V (Trigeminal) and forms muscles of mastication.",
            referenceTopic = "Pharyngeal Arch Derivatives",
            difficultyLevel = "Hard",
            bookSource = BookSnellAnatomy,
            chapterName = "Embryology"
        ),

        // Ross & Wilson Physiology - Cardiovascular
        MedicalMCQ(
            id = "pre_8",
            question = "What is the primary physiological pacemaker of the human heart, responsible for generating action potentials at a basal frequency of 60 to 100 beats per minute?",
            optionA = "Sinoatrial (SA) Node",
            optionB = "Atrioventricular (AV) Node",
            optionC = "Bundle of His",
            optionD = "Purkinje Fibers",
            correctAnswer = "A",
            explanation = "The Sinoatrial (SA) node, situated in the posterior wall of the right atrium near the opening of the superior vena cava, contains pacemaker cells that possess the fastest rate of spontaneous depolarization and thus coordinates cardiac rate.",
            referenceTopic = "Cardiac Conduction System",
            difficultyLevel = "Easy",
            bookSource = BookRossPhysiology,
            chapterName = "Cardiovascular System"
        ),
        // Ross & Wilson physiology - Nervous System
        MedicalMCQ(
            id = "pre_9",
            question = "Which cells in the Central Nervous System (CNS) are responsible for the myelination of neurons, increasing the velocity of active neural signal conduction?",
            optionA = "Oligodendrocytes",
            optionB = "Schwann Cells",
            optionC = "Astrocytes",
            optionD = "Microglia",
            correctAnswer = "A",
            explanation = "Oligodendrocytes myelinate axons in the Central Nervous System (CNS), while Schwann cells myelinate axons in the Peripheral Nervous System (PNS). Astrocytes form the blood-brain barrier, and microglia act as the resident immune defense.",
            referenceTopic = "Glial Cells and Myelination",
            difficultyLevel = "Medium",
            bookSource = BookRossPhysiology,
            chapterName = "Nervous System"
        ),
        // Ross & Wilson - Homeostasis
        MedicalMCQ(
            id = "pre_10",
            question = "In positive feedback systems of the human body, the effector amplifies the initial stimulus instead of reversing it. Which of the following is a classic example of a positive physiological feedback system?",
            optionA = "Uterine contractions driven by oxytocin release during childbirth",
            optionB = "Insulin secretion following blood glucose spikes",
            optionC = "Vasodilation in response to elevated core body temperature",
            optionD = "Negative signal inhibition of thyroid stimulating hormone",
            correctAnswer = "A",
            explanation = "Uterine contractions during labor push the baby's head down on the cervix, stimulating stretch receptors. This sends nerve signals to the brain which triggers oxytocin release. Exciting more contractions is a classic positive feedback mechanism that continues until the baby is born.",
            referenceTopic = "Homeostatic Feedback Mechanisms",
            difficultyLevel = "Easy",
            bookSource = BookRossPhysiology,
            chapterName = "Homeostasis"
        ),
        // Ross & Wilson - Cells & Tissue
        MedicalMCQ(
            id = "pre_11",
            question = "Which cell organelle is characterized as the site of oxidative phosphorylation and intracellular ATP generation via cellular respiration?",
            optionA = "Mitochondria",
            optionB = "Lysosome",
            optionC = "Golgi Apparatus",
            optionD = "Sarcoplasmic Reticulum",
            correctAnswer = "A",
            explanation = "Mitochondria are the powerhouses of cells, utilizing glucose and oxygen to generate adenosine triphosphate (ATP) through the citric acid cycle and electron transport chain.",
            referenceTopic = "Cellular Organelles",
            difficultyLevel = "Easy",
            bookSource = BookRossPhysiology,
            chapterName = "Cell Structure"
        ),
        // Ross & Wilson - Respiratory System
        MedicalMCQ(
            id = "pre_12",
            question = "What is the primary surfactant-secreting cell type lining the pulmonary alveoli, reducing surface tension to prevent respiratory alveolar collapse (atelectasis)?",
            optionA = "Type II alveolar epithelial cells (Pneumocytes)",
            optionB = "Type I alveolar epithelial cells (Pneumocytes)",
            optionC = "Alveolar Macrophages (Dust cells)",
            optionD = "Goblet cells",
            correctAnswer = "A",
            explanation = "Type II pneumocytes produce and secrete pulmonary surfactant (a lipid-protein complex). Surfactant reduces surface tension in alveolar micro-spheres, making inhalation easier and maintaining alveolar integrity during expiration.",
            referenceTopic = "Pulmonary Surfactant physiology",
            difficultyLevel = "Hard",
            bookSource = BookRossPhysiology,
            chapterName = "Respiratory System"
        ),
        // Ross & Wilson - Digestive System
        MedicalMCQ(
            id = "pre_13",
            question = "Which hormone, secreted by S-cells in the duodenum in response to acidic gastric chime entering the intestinal tract, stimulates rich bicarbonate secretion from the pancreas?",
            optionA = "Secretin",
            optionB = "Cholecystokinin (CCK)",
            optionC = "Gastrin",
            optionD = "Somatostatin",
            correctAnswer = "A",
            explanation = "Secretin is released by duodenal S-cells in response to acid. Secretin travels via blood to stimulate the pancreas to secrete bicarbonate-rich fluid into the small intestine, neutralizing gastric acid to optimize digestive enzyme function.",
            referenceTopic = "Gastrointestinal Physiology",
            difficultyLevel = "Hard",
            bookSource = BookRossPhysiology,
            chapterName = "Digestive System"
        )
    )

    val predefinedShortQuestions = listOf(
        ShortQuestion(
            id = "sq_1",
            question = "Explain the clinical significance of Hesselbach's Triangle and its boundaries.",
            baseAnswer = "Hesselbach's Triangle (Inguinal Triangle) is a clinical region bounded by: medial border - rectus abdominis muscle; lateral border - inferior epigastric vessels; inferior border - inguinal ligament. Its primary clinical significance is that direct inguinal hernias protrude directly forward through this triangle, medial to the inferior epigastric artery, due to a weakness in the conjoint tendon of the posterior abdominal wall.",
            bookSource = BookSnellAnatomy,
            chapterName = "Abdomen",
            referenceTopic = "Inguinal Hernias and Anatomy"
        ),
        ShortQuestion(
            id = "sq_2",
            question = "Describe the physiological mechanisms of the cardiac cycle, detailing Systole versus Diastole.",
            baseAnswer = "The cardiac cycle consists of: 1) Diastole (ventricular filling): Venous blood flows into the atria, AV valves (tricuspid/mitral) open, allowing ventricles to fill. The cycle finishes with atrial contraction (atrial kick). 2) Systole (ventricular contraction): Ventricles contract, raising pressure which closes the AV valves (producing first search S1 sound). When pressure exceeds aortic/pulmonary pressures, semilunar valves open, ejecting blood. The ventricles then relax, closing semilunar valves (producing second heart S2 sound).",
            bookSource = BookRossPhysiology,
            chapterName = "Cardiovascular System",
            referenceTopic = "Cardiac Cycle Execution"
        ),
        ShortQuestion(
            id = "sq_3",
            question = "Contrast Direct vs Indirect Inguinal Hernias embryologically and anatomical pathing.",
            baseAnswer = "1. Indirect Inguinal Hernia: Embryologically caused by a patent processes vaginalis. The herniating bowel enters the deep inguinal ring lateral to the inferior epigastric artery, passing down the inguinal canal in all three fascial layers. \n2. Direct Inguinal Hernia: Acquired weakness in the abdominal wall. Protrudes directly through the inguinal triangle (Hesselbach's space), medial to the inferior epigastric artery, pushing only through the conjoint tendon and superficial ring, bypassing the deep ring entirely.",
            bookSource = BookSnellAnatomy,
            chapterName = "Clinical Anatomy",
            referenceTopic = "Inguinal Hernias Differential Diagnosis"
        )
    )
}

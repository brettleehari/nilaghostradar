package com.nila.ghostradar.detection

/**
 * COCO labels that count as "a doll or toy". There is no "doll" class in COCO; "teddy bear" is the
 * v1 trigger. The day-one test (see NOTES.md) decides whether this list is enough or Phase 3 is needed.
 */
val TRIGGER_LABELS: Set<String> = setOf("teddy bear")

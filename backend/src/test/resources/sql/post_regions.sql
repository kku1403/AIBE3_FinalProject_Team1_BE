SET
FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE post_region;
SET
FOREIGN_KEY_CHECKS = 1;

INSERT INTO post_region (id, post_id, region_id, created_at, modified_at)
VALUES (1, 1, 3, NOW(), NOW()),
       (2, 2, 5, NOW(), NOW()),
       (3, 3, 4, NOW(), NOW());

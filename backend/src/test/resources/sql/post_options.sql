SET
FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE post_option;
SET
FOREIGN_KEY_CHECKS = 1;

INSERT INTO post_option (id, name, deposit, fee, post_id, created_at, modified_at)
VALUES (1, '충전기 포함', 0, 0, 2, NOW(), NOW()),
       (2, '배터리 2개', 0, 0, 1, NOW(), NOW()),
       (3, '풋살용 공', 0, 0, 3, NOW(), NOW());
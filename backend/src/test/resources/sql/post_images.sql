SET
FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE post_image;
SET
FOREIGN_KEY_CHECKS = 1;

INSERT INTO post_image (id, image_url, is_primary, post_id, created_at, modified_at)
VALUES (1, 'https://test.com/go_pro_1.jpg', TRUE, 1, NOW(), NOW()),
       (2, 'https://test.com/go_pro_2.jpg', FALSE, 1, NOW(), NOW()),
       (3, 'https://test.com/macbook.jpg', TRUE, 2, NOW(), NOW()),
       (4, 'https://test.com/football.jpg', TRUE, 3, NOW(), NOW());
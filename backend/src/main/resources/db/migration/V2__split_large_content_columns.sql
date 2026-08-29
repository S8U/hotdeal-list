CREATE TABLE `hotdeal_raw_contents` (
    `hotdeal_raw_id` BIGINT NOT NULL COMMENT '핫딜 원본 ID',
    `content_html` TEXT DEFAULT NULL COMMENT '게시글 본문 HTML',
    PRIMARY KEY (`hotdeal_raw_id`),
    CONSTRAINT `fk_raw_contents_raw`
        FOREIGN KEY (`hotdeal_raw_id`) REFERENCES `hotdeal_raws` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='핫딜 원본 본문';

CREATE TABLE `hotdeal_process_contents` (
    `hotdeal_process_id` BIGINT NOT NULL COMMENT '핫딜 가공 데이터 ID',
    `ai_prompt` TEXT NOT NULL COMMENT 'AI 요청 프롬프트',
    `ai_response` TEXT NOT NULL COMMENT 'AI 응답 원본',
    PRIMARY KEY (`hotdeal_process_id`),
    CONSTRAINT `fk_process_contents_process`
        FOREIGN KEY (`hotdeal_process_id`) REFERENCES `hotdeal_processes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='핫딜 AI 가공 원문';

INSERT INTO `hotdeal_raw_contents` (`hotdeal_raw_id`, `content_html`)
SELECT `id`, `content_html`
FROM `hotdeal_raws`
WHERE `content_html` IS NOT NULL;

INSERT INTO `hotdeal_process_contents` (`hotdeal_process_id`, `ai_prompt`, `ai_response`)
SELECT `id`, `ai_prompt`, `ai_response`
FROM `hotdeal_processes`;

ALTER TABLE `hotdeal_raws`
    DROP COLUMN `content_html`;

ALTER TABLE `hotdeal_processes`
    DROP COLUMN `ai_prompt`,
    DROP COLUMN `ai_response`;

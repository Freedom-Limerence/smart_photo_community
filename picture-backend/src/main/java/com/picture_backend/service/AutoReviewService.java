package com.picture_backend.service;

import com.picture_backend.model.entity.Picture;
import com.picture_backend.model.entity.User;

import java.util.Map;

/**
 * 机审（自动审核）：用可解释的规则替代"每张都人工审"，只把高风险内容转给人工。
 *
 * <p>分层策略：机审通过 -> 直接进入公共图库；命中风险 -> 转人工（并写明原因）。</p>
 */
public interface AutoReviewService {

    /** 单张机审判定（上传/编辑时调用），不落库 */
    Decision decide(Picture picture, User author);

    /** 批量机审积压的待审核图片（管理端手动触发） */
    Map<String, Object> reviewPending(int limit);

    Map<String, Object> getConfig();

    void saveConfig(Map<String, Object> config);

    /** 机审统计：待人工 / 机审通过 / 人工通过 等 */
    Map<String, Object> stats();

    class Decision {
        /** 1 = 机审通过；0 = 转人工 */
        private final Integer reviewStatus;
        private final String message;
        private final String rule;
        private final boolean auto;

        public Decision(Integer reviewStatus, String message, String rule, boolean auto) {
            this.reviewStatus = reviewStatus;
            this.message = message;
            this.rule = rule;
            this.auto = auto;
        }

        public Integer getReviewStatus() { return reviewStatus; }
        public String getMessage() { return message; }
        public String getRule() { return rule; }
        public boolean isAuto() { return auto; }
    }
}

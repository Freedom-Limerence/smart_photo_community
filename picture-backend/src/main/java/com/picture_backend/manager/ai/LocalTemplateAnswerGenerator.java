package com.picture_backend.manager.ai;

import com.picture_backend.model.entity.AiMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 本地模板生成器：把工具检索到的真实数据组织成回答。
 *
 * <p>原则：只说检索到的事实，命中为空时明确说明没有找到，不编造图片或摄影师。</p>
 */
@Component
public class LocalTemplateAnswerGenerator implements AnswerGenerator {

    @Override
    public String name() {
        return "LOCAL_TEMPLATE";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String generate(String question, ChatIntent intent, List<ToolResult> results,
                           List<AiMessage> history, Consumer<String> onDelta) {
        String answer = build(intent, results);
        String[] lines = answer.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            onDelta.accept(lines[i] + (i < lines.length - 1 ? "\n" : ""));
            try {
                Thread.sleep(30L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return answer;
    }

    private String build(ChatIntent intent, List<ToolResult> results) {
        ToolResult first = results.isEmpty() ? null : results.get(0);
        if (first == null) {
            return helpAnswer();
        }
        String type = intent.getType() == null ? "HELP" : intent.getType();
        switch (type) {
            case "SEARCH_WORKS":
                return workAnswer(first);
            case "SEARCH_PICTURES":
                return searchAnswer(first, intent);
            case "FIND_PHOTOGRAPHERS":
                return photographerAnswer(first);
            case "LIST_EXHIBITIONS":
                return exhibitionAnswer(first);
            case "PLATFORM_STATS":
                return statsAnswer(first);
            case "SIMILAR":
                return similarAnswer(first);
            case "DETAIL":
                return detailAnswer(first);
            default:
                return helpAnswer();
        }
    }

    private String searchAnswer(ToolResult result, ChatIntent intent) {
        StringBuilder sb = new StringBuilder();
        sb.append("我在平台里").append(result.getSummary()).append("。\n");
        if (result.getReferences().isEmpty()) {
            sb.append("没有符合条件的图片：可能是还没上传这类照片，或者它们还没通过审核。\n");
            sb.append("你可以换一个题材（人像 / 建筑 / 夜景），或者先上传几张，我再帮你编目和检索。");
            return sb.toString();
        }
        int index = 1;
        for (var ref : result.getReferences()) {
            sb.append(index++).append(". 《").append(ref.getTitle()).append("》");
            if (StringUtils.hasText(ref.getReason())) {
                sb.append(" —— ").append(ref.getReason());
            }
            sb.append("\n");
        }
        sb.append("这些结果全部来自平台内已入库并有权查看的图片（权限过滤先于排序），点击卡片可以直接打开。");
        if (intent.slot("mood") != null) {
            sb.append("\n想换个风格的话，可以试试「明亮」或「暖色调」。");
        }
        return sb.toString();
    }

    private String workAnswer(ToolResult result) {
        if (result.getReferences().isEmpty()) {
            return "我查过了社区公开作品，暂时没有找到符合这次描述的创作。可以换个题材或风格再试一次。";
        }
        StringBuilder answer = new StringBuilder("在社区里找到 " + result.getReferences().size() + " 组相关作品。你可以点开下面的卡片，沉浸浏览作者的完整创作：\n");
        int index = 1;
        for (var ref : result.getReferences()) {
            answer.append(index++).append(". 《").append(ref.getTitle()).append("》");
            if (StringUtils.hasText(ref.getSubtitle())) answer.append(" · ").append(ref.getSubtitle());
            answer.append("\n");
        }
        return answer.toString();
    }

    private String photographerAnswer(ToolResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(result.getSummary()).append("：\n");
        if (result.getReferences().isEmpty()) {
            sb.append("暂时没有匹配的摄影师，可能是平台上还没有这类公开作品。");
            return sb.toString();
        }
        int index = 1;
        for (var ref : result.getReferences()) {
            sb.append(index++).append(". ").append(ref.getTitle());
            if (StringUtils.hasText(ref.getSubtitle())) {
                sb.append("（").append(ref.getSubtitle()).append("）");
            }
            sb.append(" —— ").append(ref.getReason()).append("\n");
        }
        sb.append("点击可以进入 TA 的主页查看作品。");
        return sb.toString();
    }

    private String exhibitionAnswer(ToolResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(result.getSummary()).append("：\n");
        for (var ref : result.getReferences()) {
            sb.append("- ").append(ref.getTitle());
            if (StringUtils.hasText(ref.getSubtitle())) {
                sb.append("（").append(ref.getSubtitle()).append("）");
            }
            sb.append("\n");
        }
        if (result.getReferences().isEmpty()) {
            sb.append("目前还没有已发布的专题，管理员发布后我会第一时间告诉你。");
        }
        return sb.toString();
    }

    private String statsAnswer(ToolResult result) {
        Object item = result.getItems().isEmpty() ? null : result.getItems().get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("这是平台当前的真实数据：\n");
        if (item instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) item;
            sb.append("- 用户：").append(m.get("userCount")).append(" 人\n");
            sb.append("- 审核通过的公开图片：").append(m.get("publicPictureCount")).append(" 张\n");
            sb.append("- 已发布作品：").append(m.get("publishedWorkCount")).append(" 件\n");
            sb.append("- 已发布专题：").append(m.get("publishedExhibitionCount")).append(" 个");
        }
        return sb.toString();
    }

    private String similarAnswer(ToolResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(result.getSummary()).append("。\n");
        if (!result.getReferences().isEmpty()) {
            sb.append("分组结果：\n");
            for (var ref : result.getReferences()) {
                sb.append("- 《").append(ref.getTitle()).append("》").append(ref.getReason()).append("\n");
            }
        }
        sb.append("提示：相似分组只给选片建议，不会自动删除任何原图。");
        return sb.toString();
    }

    private String detailAnswer(ToolResult result) {
        Object item = result.getItems().isEmpty() ? null : result.getItems().get(0);
        StringBuilder sb = new StringBuilder();
        sb.append(result.getSummary()).append("\n");
        if (item instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) item;
            List<String> lines = new ArrayList<>();
            if (m.get("cameraModel") != null) {
                lines.add("相机：" + m.get("cameraMake") + " " + m.get("cameraModel"));
            }
            if (m.get("lensModel") != null) {
                lines.add("镜头：" + m.get("lensModel"));
            }
            if (m.get("shootTime") != null) {
                lines.add("拍摄时间：" + m.get("shootTime"));
            }
            if (m.get("subjectTags") != null) {
                lines.add("主体标签：" + m.get("subjectTags"));
            }
            if (m.get("picColor") != null) {
                lines.add("主色：" + m.get("picColor"));
            }
            if (lines.isEmpty()) {
                sb.append("这张图片还没有摄影元数据，你可以在「摄影编目」里为它生成一次。");
            } else {
                for (String l : lines) {
                    sb.append("- ").append(l).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private String helpAnswer() {
        return "我是光影集的摄影助手，可以帮你：\n"
                + "- 找图：例如「帮我找几张适合做壁纸的深色系风景图」\n"
                + "- 找人：例如「平台上有哪些擅长拍人像的摄影师」\n"
                + "- 看专题：例如「最近有什么专题展览」\n"
                + "- 看数据：例如「平台现在有多少张图片」\n"
                + "- 看图：在图片详情页问我「这张照片什么参数」「找相似的连拍」\n"
                + "所有回答都基于平台里真实存在的图片和作品，不会编造。";
    }
}

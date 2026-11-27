/*
 Navicat Premium Dump SQL

 Source Server         : 172.16.75.105
 Source Server Type    : MySQL
 Source Server Version : 80035 (8.0.35)
 Source Host           : 172.16.75.105:3306
 Source Schema         : travel_rag

 Target Server Type    : MySQL
 Target Server Version : 80035 (8.0.35)
 File Encoding         : 65001

 Date: 27/11/2026 18:39:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for travel_knowledge_chunk
-- ----------------------------
DROP TABLE IF EXISTS `travel_knowledge_chunk`;
CREATE TABLE `travel_knowledge_chunk` (
  `id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `source` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `scope` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'GLOBAL',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of travel_knowledge_chunk
-- ----------------------------
BEGIN;
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('global-food-guidance-1', '饮食节奏与预算\n保持规律三餐，按时吃饭，避免为了赶行程长时间空腹或暴饮暴食。餐饮预算可以按基础餐、特色餐和饮水零食分别估算。热门店铺的排队和等位时间应计入行程，价格与营业时间需在就餐前实时核实。', '2026-11-27 18:19:53.511817', 'global-food-guidance.md', '通用旅行饮食指南 - 饮食节奏与预算', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('global-food-guidance-2', '食品卫生与热量\n选择卫生条件稳定、食材充分加热的餐饮场所。生食、凉拌、海鲜和乳制品需要结合保存条件与个人肠胃情况谨慎选择。天气炎热时缩短易腐食品的携带时间，出现异味或保存不当应停止食用。', '2026-11-27 18:19:53.511817', 'global-food-guidance.md', '通用旅行饮食指南 - 食品卫生与热量', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('global-food-guidance-3', '过敏原与特殊饮食\n对花生、坚果、乳制品、麸质、海鲜等过敏时，提前准备书面说明并主动询问配料和交叉接触风险。素食、清真或宗教饮食需求应确认餐厅是否具备相应条件，不能只依据菜品名称判断。', '2026-11-27 18:19:53.511817', 'global-food-guidance.md', '通用旅行饮食指南 - 过敏原与特殊饮食', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('global-food-guidance-4', '饮水肠胃与就医\n旅行期间适量饮水，避免突然改变饮食结构。出现持续呕吐、腹泻、发热或脱水症状时应停止尝试新食物，及时补充水分并寻求正规医疗帮助。保留就诊记录和保险资料，按医生建议用药。', '2026-11-27 18:19:53.511817', 'global-food-guidance.md', '通用旅行饮食指南 - 饮水肠胃与就医', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('global-itinerary-principles-1', '明确目标与约束\n先确认旅行目标、日期、人数、预算、体力水平和必须完成的事项，再安排具体活动。亲子、长辈、无障碍或短期商务旅行需要不同的节奏与设施条件。信息不足时先澄清，不要默认某个城市或某种旅行方式。', '2026-11-27 18:19:53.511817', 'global-itinerary-principles.md', '通用行程规划原则 - 明确目标与约束', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('global-itinerary-principles-2', '控制节奏与负荷\n每天设置一个主要目标和少量可选活动，避免把多个远距离区域塞进同一天。游览、用餐、交通和休息都要占用时间，连续高强度活动后应安排恢复时段。遇到排队、延误或天气变化时，优先删除可选项目。', '2026-11-27 18:19:53.511817', 'global-itinerary-principles.md', '通用行程规划原则 - 控制节奏与负荷', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('global-itinerary-principles-3', '按地理区域组织\n把位置相近的景点、餐厅和住宿安排在同一时段，跨区域移动单独计算时间与费用。地图上的直线距离不等于实际通行时间，山区、岛屿、景区接驳和换乘会增加额外成本。具体路线应以目的地官方交通信息为准。', '2026-11-27 18:19:53.511817', 'global-itinerary-principles.md', '通用行程规划原则 - 按地理区域组织', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('global-itinerary-principles-4', '首尾日与备选方案\n抵达日和返程日减少固定预约项目，为值机、取行李、进城和安检预留缓冲。每日准备一个室内或就近的备选方案，并提前确认取消、改签和预约规则。临时信息变化时，以官方渠道的最新通知为准。', '2026-11-27 18:19:53.511817', 'global-itinerary-principles.md', '通用行程规划原则 - 首尾日与备选方案', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('global-transport-accommodation-1', '城际交通比较\n比较飞机、火车、长途汽车等方式时，不只看票价，还要计算机场或车站往返、行李、选座、接驳和时间成本。核对退改签规则、儿童票条件和最晚到达时间。高峰时段应预留更充分的换乘缓冲。', '2026-11-27 18:19:53.511817', 'global-transport-accommodation.md', '通用交通与住宿选择 - 城际交通比较', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('global-transport-accommodation-2', '市内交通规划\n根据当天活动区域组合步行、公共交通、出租车或租车。公共交通需要核对首末班车、换乘和支付方式；租车需要确认驾照、保险、停车、通行限制和还车要求。夜间或携带行李时优先选择安全、可追踪的交通方式。', '2026-11-27 18:19:53.511817', 'global-transport-accommodation.md', '通用交通与住宿选择 - 市内交通规划', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('global-transport-accommodation-3', '住宿位置与总成本\n住宿总成本包括房价、税费、清洁费、押金、早餐、取消政策和往返交通。位置较偏但交通便利的住宿可能比核心区域更划算。预订前核对床型、入住人数、儿童政策和实际评价，不只看宣传图片。', '2026-11-27 18:19:53.511817', 'global-transport-accommodation.md', '通用交通与住宿选择 - 住宿位置与总成本', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('global-transport-accommodation-4', '入住安全与契约核对\n入住时检查门锁、消防通道、卫生条件和贵重物品保管方式。保留预订确认、付款记录和酒店联系方式。遇到临时加价、房型不符或安全隐患时，先与前台核实，必要时联系预订平台或当地正规投诉渠道。', '2026-11-27 18:19:53.511817', 'global-transport-accommodation.md', '通用交通与住宿选择 - 入住安全与契约核对', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('travel-budget-1', '预算结构\n旅行预算通常包括城际交通、住宿、餐饮、市内交通、门票、购物和弹性费用。先确定总预算和优先级，再按项目分配。预算应写明人数、日期、交通方式、住宿档次等假设，不能把估算值当作实时报价。', '2026-11-24 15:10:23.434767', 'travel-budget.md', '旅行预算估算方法 - 预算结构', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('travel-budget-2', '城际交通预算\n比较往返机票、火车票和长途汽车时，同时计入行李、选座、接送和换乘成本。节假日、周末和临近出发日期的价格可能变化。退改签费用也应纳入比较，优先选择总成本与出行需求更匹配的方案。', '2026-11-24 15:10:23.434767', 'travel-budget.md', '旅行预算估算方法 - 城际交通预算', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('travel-budget-3', '住宿预算\n住宿费用不只有房价，还可能包含税费、清洁费、押金和取消成本。多人同行时比较房间数与床位组合，位置较偏的住宿要计入额外交通时间和费用。预订前核对入住人数、床型和儿童政策。', '2026-11-24 15:10:23.434767', 'travel-budget.md', '旅行预算估算方法 - 住宿预算', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('travel-budget-4', '餐饮门票与体验\n餐饮可以按每日基础餐饮、特色餐和饮水零食估算。门票先列出必去项目，再核对官方票价、预约规则和优惠政策。体验项目、导游、演出和购物应单独列项，避免与必需支出混在一起。', '2026-11-24 15:10:23.434767', 'travel-budget.md', '旅行预算估算方法 - 餐饮门票与体验', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('travel-budget-5', '弹性费用与价格核实\n建议预留总预算的百分之十到二十作为弹性费用，用于天气变化、临时交通、医疗或行程调整。所有价格都应在出行前通过官方渠道重新核实。预算紧张时优先削减可选购物和高成本体验，不要压缩安全与基本休息费用。', '2026-11-24 15:10:23.434767', 'travel-budget.md', '旅行预算估算方法 - 弹性费用与价格核实', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('travel-safety-1', '信息核实\n交通班次、票价、天气、签证政策、景区开放时间和预约规则都可能变化。知识库提供的是通用方法，不代表实时信息。出发前应通过交通运营方、景区、气象、使领馆和卫生部门等官方渠道核实。', '2026-11-24 15:10:23.434767', 'travel-safety.md', '旅行安全与出行提醒 - 信息核实', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('travel-safety-2', '天气与高原\n不要仅凭季节判断目的地天气，应查看临近日期的天气预报和预警。前往高原地区需要提前了解海拔、适应要求和医疗条件，避免剧烈活动。出现明显身体不适时应停止行程并寻求医疗帮助。', '2026-11-24 15:10:23.434767', 'travel-safety.md', '旅行安全与出行提醒 - 天气与高原', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('travel-safety-3', '证件与保险\n随身携带有效身份证件，国际旅行还应确认护照、签证和入境要求。根据行程考虑旅行保险，特别是医疗、行程变更和行李风险。重要证件可以准备电子备份，但原件仍需妥善保管。', '2026-11-24 15:10:23.434767', 'travel-safety.md', '旅行安全与出行提醒 - 证件与保险', '2026-11-24 15:10:23.434767', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('travel-safety-4', '财物与通讯\n在人多场所看管手机、钱包和行李，避免公开展示大量现金。提前保存住宿地址、紧急联系方式和保险信息。保持手机电量，必要时携带充电宝，并遵守交通工具的携带规定。', '2026-11-24 15:10:23.434767', 'travel-safety.md', '旅行安全与出行提醒 - 财物与通讯', '2026-11-27 18:19:53.511817', 'GLOBAL');
INSERT INTO `travel_knowledge_chunk` (`id`, `content`, `created_at`, `source`, `title`, `updated_at`, `scope`) VALUES ('travel-safety-5', '健康与饮食\n准备常用药品并注意保质期。异地饮食应适量，避免暴饮暴食。对药物、食物或昆虫过敏的人应携带相应应急物品，并提前了解附近正规医疗机构位置。持续或严重症状应及时就医。', '2026-11-24 15:10:23.434767', 'travel-safety.md', '旅行安全与出行提醒 - 健康与饮食', '2026-11-27 18:19:53.511817', 'GLOBAL');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

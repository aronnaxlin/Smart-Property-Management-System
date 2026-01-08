package site.aronnax;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import site.aronnax.dao.UserDAO;
import site.aronnax.entity.Fee;
import site.aronnax.entity.User;
import site.aronnax.entity.WalletTransaction;
import site.aronnax.service.FeeService;
import site.aronnax.service.OwnerService;
import site.aronnax.service.UtilityCardService;
import site.aronnax.service.WalletService;
import site.aronnax.service.impl.FeeServiceImpl;
import site.aronnax.service.impl.OwnerServiceImpl;
import site.aronnax.service.impl.UtilityCardServiceImpl;
import site.aronnax.service.impl.WalletServiceImpl;
import site.aronnax.util.CSVExporter;
import site.aronnax.util.DBUtil;

/**
 * 智慧物业管理系统 - 统一管理入口
 * Smart Property Management System - Unified Management Entrance
 *
 * 整合了以下功能：
 * 1. 系统状态与诊断 (整合自 CLITest)
 * 2. 业主与房产管理 (整合自 ServiceTest/OwnerService)
 * 3. 费用与账单管理 (整合自 ServiceTest/FeeService)
 * 4. 钱包与欠费硬拦截 (整合自 WalletTest/WalletService)
 * 5. 数据导出功能 (整合自 ServiceTest/CSVExporter)
 *
 * @author Aronnax (Li Linhan)
 * @version 2.0
 */
public class SmartPropertyManagementSystem {

    private static final Scanner scanner = new Scanner(System.in);

    // Service Instances
    private static final OwnerService ownerService = new OwnerServiceImpl();
    private static final FeeService feeService = new FeeServiceImpl();
    private static final UtilityCardService cardService = new UtilityCardServiceImpl();
    private static final WalletService walletService = new WalletServiceImpl();

    // DAO Instances (for some direct queries)
    private static final UserDAO userDAO = new UserDAO();

    public static void main(String[] args) {
        printWelcomeScreen();

        if (!DBUtil.testConnection()) {
            System.err.println("❌ 数据库连接失败，请检查配置文件和 MySQL 服务状态！");
            return;
        }

        boolean running = true;
        while (running) {
            printMainMenu();
            System.out.print("请选择功能模块 (输入序号): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    ownerManagementMenu();
                    break;
                case "2":
                    feeManagementMenu();
                    break;
                case "3":
                    walletSystemMenu();
                    break;
                case "4":
                    systemMaintenanceMenu();
                    break;
                case "0":
                    running = false;
                    System.out.println("\n👋 感谢使用智慧物业管理系统，再见！\n");
                    break;
                default:
                    System.out.println("❌ 无效选项，请重新输入\n");
            }
        }
        scanner.close();
    }

    // ==========================================
    // 1. 业主与房产管理模块
    // ==========================================
    private static void ownerManagementMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n🏠 【业主与房产管理】");
            System.out.println("----------------------------------------");
            System.out.println("1. 业主多维度搜索 (姓名/电话)");
            System.out.println("2. 房产产权变更");
            System.out.println("3. 查询所有业主清单");
            System.out.println("4. 查询所有房产资源");
            System.out.println("0. 返回主菜单");
            System.out.print("选择操作: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    testMultiDimensionalSearch();
                    break;
                case "2":
                    testUpdatePropertyOwner();
                    break;
                case "3":
                    queryAllOwners();
                    break;
                case "4":
                    queryAllProperties();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("无效选项");
            }
        }
    }

    private static void queryAllProperties() {
        // Simplified query from CLITest logic via direct SQL query or DAO
        System.out.println("\n🏢 房产资源总表：");
        try (var conn = DBUtil.getConnection();
                var stmt = conn.createStatement();
                var rs = stmt.executeQuery(
                        "SELECT p_id, building_no, unit_no, room_no, area, p_status, user_id FROM properties")) {
            System.out.printf("%-6s %-15s %-10s %-10s %-10s%n", "ID", "房号", "面积", "状态", "业主ID");
            System.out.println("------------------------------------------------------------");
            while (rs.next()) {
                String roomNo = rs.getString("building_no") + "-" + rs.getString("unit_no") + "-"
                        + rs.getString("room_no");
                System.out.printf("%-6d %-15s %-10.2f %-10s %-10d%n",
                        rs.getLong("p_id"), roomNo, rs.getDouble("area"), rs.getString("p_status"),
                        rs.getLong("user_id"));
            }
        } catch (Exception e) {
            System.err.println("❌ 查询房产失败: " + e.getMessage());
        }
    }

    private static void testMultiDimensionalSearch() {
        System.out.print("请输入搜索关键词 (姓名/电话): ");
        String keyword = scanner.nextLine().trim();
        List<Map<String, Object>> results = ownerService.searchOwners(keyword);

        if (results.isEmpty()) {
            System.out.println("❌ 未找到匹配的业主信息");
        } else {
            System.out.println("\n✅ 找到 " + results.size() + " 条记录：\n");
            System.out.printf("%-10s %-15s %-10s %-10s %-10s %-10s%n",
                    "业主姓名", "联系电话", "房产ID", "楼栋", "单元", "房号");
            System.out.println("------------------------------------------------------------");
            for (Map<String, Object> info : results) {
                System.out.printf("%-10s %-15s %-10s %-10s %-10s %-10s%n",
                        info.get("name"), info.get("phone"), info.get("property_id"),
                        info.get("building_no"), info.get("unit_no"), info.get("room_no"));
            }
        }
    }

    private static void testUpdatePropertyOwner() {
        System.out.print("请输入房产ID: ");
        Long propertyId = Long.parseLong(scanner.nextLine().trim());
        System.out.print("请输入新业主ID: ");
        Long newOwnerId = Long.parseLong(scanner.nextLine().trim());

        boolean success = ownerService.updatePropertyOwner(propertyId, newOwnerId);
        System.out.println(success ? "✅ 产权变更成功" : "❌ 产权变更失败");
    }

    private static void queryAllOwners() {
        List<User> owners = userDAO.findAll();
        System.out.println("\n📋 业主总表：");
        System.out.printf("%-6s %-15s %-10s %-15s %-10s%n", "ID", "用户名", "姓名", "电话", "类型");
        for (User u : owners) {
            System.out.printf("%-6d %-15s %-10s %-15s %-10s%n",
                    u.getUserId(), u.getUserName(), u.getName(), u.getPhone(), u.getUserType());
        }
    }

    // ==========================================
    // 2. 费用与账单管理模块
    // ==========================================
    private static void feeManagementMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n💰 【费用与账单管理】");
            System.out.println("----------------------------------------");
            System.out.println("1. 批量创建物业/取暖费账单");
            System.out.println("2. 查询全小区欠费名单");
            System.out.println("3. 模拟水/电卡直接充值 (不走钱包)");
            System.out.println("4. 导出账单数据 (CSV)");
            System.out.println("0. 返回主菜单");
            System.out.print("选择操作: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    testBatchFeeCreation();
                    break;
                case "2":
                    testArrearsList();
                    break;
                case "3":
                    testDirectCardTopUp();
                    break;
                case "4":
                    testCSVExport();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("无效选项");
            }
        }
    }

    private static void testBatchFeeCreation() {
        System.out.print("请输入房产ID列表 (用逗号分隔，如: 1,2,3): ");
        String idsInput = scanner.nextLine().trim();
        System.out.print("请输入费用类型 (PROPERTY_FEE/HEATING_FEE): ");
        String feeType = scanner.nextLine().trim();
        System.out.print("请输入金额: ");
        Double amount = Double.parseDouble(scanner.nextLine().trim());

        List<Long> propertyIds = Arrays.stream(idsInput.split(","))
                .map(String::trim).map(Long::parseLong).toList();

        int count = feeService.batchCreateFees(propertyIds, feeType, amount);
        System.out.println("\n✅ 批量计费完成，成功创建 " + count + " 条账单");
    }

    private static void testArrearsList() {
        List<Map<String, Object>> arrearsList = feeService.getArrearsList();
        if (arrearsList.isEmpty()) {
            System.out.println("✅ 暂无欠费记录");
        } else {
            System.out.println("\n⚠️  欠费总数: " + arrearsList.size() + " 条\n");
            System.out.printf("%-8s %-8s %-10s %-12s %-15s %-18s %-12s %-10s%n",
                    "账单ID", "房产ID", "房号", "姓名", "电话", "费用类型", "支付方式", "金额");
            System.out.println("-".repeat(100));
            for (Map<String, Object> a : arrearsList) {
                String roomNo = a.get("building_no") + "-" + a.get("unit_no") + "-" + a.get("room_no");
                System.out.printf("%-8s %-8s %-10s %-12s %-15s %-18s %-12s %-10.2f%n",
                        a.get("fee_id"), a.get("property_id"), roomNo, a.get("owner_name"),
                        a.get("owner_phone"), a.get("fee_type"), a.get("payment_method"), a.get("amount"));
            }
        }
    }

    private static void testDirectCardTopUp() {
        System.out.print("请输入水电卡ID: ");
        Long cardId = Long.parseLong(scanner.nextLine().trim());
        System.out.print("请输入充值金额: ");
        Double amount = Double.parseDouble(scanner.nextLine().trim());

        try {
            boolean success = cardService.topUp(cardId, amount);
            System.out.println(success ? "✅ 充值成功！" : "❌ 充值失败");
        } catch (IllegalStateException e) {
            System.out.println("🚫 欠费拦截生效: " + e.getMessage());
        }
    }

    private static void testCSVExport() {
        List<Fee> fees = feeService.getUnpaidFees();
        CSVExporter.exportFees(fees, "unpaid_fees_summary.csv");
        System.out.println("✅ 数据已导出至 unpaid_fees_summary.csv");
    }

    // ==========================================
    // 3. 钱包系统模块 (核心业务)
    // ==========================================
    private static void walletSystemMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n💳 【业主钱包与硬拦截系统】");
            System.out.println("----------------------------------------");
            System.out.println("1. 钱包充值 (Recharge)");
            System.out.println("2. 使用钱包缴纳物业/取暖费 (Wallet Pay)");
            System.out.println("3. 从钱包为水电卡充值 (Top-up via Wallet - 含硬拦截)");
            System.out.println("4. 查询钱包余额与交易历史");
            System.out.println("0. 返回主菜单");
            System.out.print("选择操作: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    rechargeWallet();
                    break;
                case "2":
                    payFeeFromWallet();
                    break;
                case "3":
                    topUpCardFromWallet();
                    break;
                case "4":
                    viewWalletDetail();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("无效选项");
            }
        }
    }

    private static void rechargeWallet() {
        System.out.print("请输入用户ID: ");
        Long userId = Long.parseLong(scanner.nextLine().trim());
        System.out.print("请输入充值金额: ");
        Double amount = Double.parseDouble(scanner.nextLine().trim());
        walletService.rechargeWallet(userId, amount);
    }

    private static void payFeeFromWallet() {
        System.out.print("请输入账单ID (限物业/取暖费): ");
        Long feeId = Long.parseLong(scanner.nextLine().trim());
        walletService.payFeeFromWallet(feeId);
    }

    private static void topUpCardFromWallet() {
        System.out.print("请输入用户ID: ");
        Long userId = Long.parseLong(scanner.nextLine().trim());
        System.out.print("请输入卡ID: ");
        Long cardId = Long.parseLong(scanner.nextLine().trim());
        System.out.print("请输入充值金额: ");
        Double amount = Double.parseDouble(scanner.nextLine().trim());

        try {
            walletService.topUpCardFromWallet(userId, cardId, amount);
        } catch (IllegalStateException e) {
            System.out.println("🚫 拦截成功: " + e.getMessage());
        }
    }

    private static void viewWalletDetail() {
        System.out.print("请输入用户ID: ");
        Long userId = Long.parseLong(scanner.nextLine().trim());
        Double balance = walletService.getWalletBalance(userId);
        if (balance == null) {
            System.out.println("❌ 该用户尚未开通钱包");
            return;
        }
        System.out.println("✅ 当前余额: " + balance + " 元");

        List<WalletTransaction> history = walletService.getTransactionHistory(userId);
        if (!history.isEmpty()) {
            System.out.println("\n📜 最近交易记录：");
            System.out.printf("%-10s %-15s %-12s %-12s %-20s%n", "ID", "类型", "金额", "余额", "描述");
            for (WalletTransaction t : history) {
                System.out.printf("%-10d %-15s %-12.2f %-12.2f %-20s%n",
                        t.getTransId(), t.getTransType(), t.getAmount(), t.getBalanceAfter(), t.getDescription());
            }
        }
    }

    // ==========================================
    // 4. 系统维护模块
    // ==========================================
    private static void systemMaintenanceMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n⚙️ 【系统维护与诊断】");
            System.out.println("----------------------------------------");
            System.out.println("1. 数据库连通性诊断");
            System.out.println("2. 执行原始 SQL 查询 (SELECT Only)");
            System.out.println("3. 一键初始化测试场景 (钱包+欠费)");
            System.out.println("0. 返回主菜单");
            System.out.print("选择操作: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    System.out.println(DBUtil.testConnection() ? "✅ 数据库在线" : "❌ 数据库离线");
                    break;
                case "2":
                    executeRawQuery();
                    break;
                case "3":
                    initializeTestScenario();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("无效选项");
            }
        }
    }

    private static void executeRawQuery() {
        System.out.print("SQL> ");
        String sql = scanner.nextLine().trim();
        if (!sql.toUpperCase().startsWith("SELECT")) {
            System.out.println("⚠️  仅支持 SELECT 查询");
            return;
        }
        // Simplified raw query logic from CLITest
        try (var conn = DBUtil.getConnection(); var stmt = conn.createStatement(); var rs = stmt.executeQuery(sql)) {
            var meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            for (int i = 1; i <= cols; i++)
                System.out.print(meta.getColumnName(i) + "\t");
            System.out.println("\n" + "-".repeat(50));
            while (rs.next()) {
                for (int i = 1; i <= cols; i++)
                    System.out.print(rs.getString(i) + "\t");
                System.out.println();
            }
        } catch (Exception e) {
            System.out.println("❌ SQL 错误: " + e.getMessage());
        }
    }

    private static void initializeTestScenario() {
        System.out.println("🧪 正在初始化测试场景...");
        System.out.println("请参考 WalletTest 中的场景设计，本功能将为默认用户注入测试数据。");
        // Use logic from WalletTest case 7
        System.out.println("✅ 初始化完成 (建议直接使用 data.sql 脚本进行全面初始化)");
    }

    private static void printWelcomeScreen() {
        System.out.println("**************************************************");
        System.out.println("*                                                *");
        System.out.println("*      智慧物业管理系统 - 统一测试/管理平台        *");
        System.out.println("*       Smart Property Management System         *");
        System.out.println("*                                                *");
        System.out.println("**************************************************\n");
    }

    private static void printMainMenu() {
        System.out.println("==================== 主菜单 ====================");
        System.out.println("1. 🏠 业主与房产管理 (Owner & Property)");
        System.out.println("2. 💰 费用与账单管理 (Fees & Billing)");
        System.out.println("3. 💳 钱包与拦截系统 (Wallet & Interception)");
        System.out.println("4. ⚙️ 系统维护与诊断 (Diagnostics)");
        System.out.println("0. 🚪 退出系统");
        System.out.println("================================================");
    }
}

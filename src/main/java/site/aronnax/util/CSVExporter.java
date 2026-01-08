package site.aronnax.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import site.aronnax.entity.Fee;
import site.aronnax.entity.User;

/**
 * CSV Exporter Utility
 * Provides CSV export functionality for various data
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public class CSVExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export owner information to CSV
     *
     * @param owners   List of users (owners)
     * @param filePath Output file path
     * @return true if successful
     */
    public static boolean exportOwners(List<User> owners, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Write CSV header
            writer.write("User ID,User Name,Name,Gender,Phone,User Type,Created At");
            writer.newLine();

            // Write data rows
            for (User owner : owners) {
                StringBuilder line = new StringBuilder();
                line.append(owner.getUserId()).append(",");
                line.append(csvEscape(owner.getUserName())).append(",");
                line.append(csvEscape(owner.getName())).append(",");
                line.append(csvEscape(owner.getGender())).append(",");
                line.append(csvEscape(owner.getPhone())).append(",");
                line.append(csvEscape(owner.getUserType())).append(",");
                line.append(owner.getCreatedAt() != null ? owner.getCreatedAt().format(DATE_FORMATTER) : "");

                writer.write(line.toString());
                writer.newLine();
            }

            System.out.println("✅ 业主信息已导出至: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("❌ 导出业主信息失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * Export fee information to CSV
     *
     * @param fees     List of fees
     * @param filePath Output file path
     * @return true if successful
     */
    public static boolean exportFees(List<Fee> fees, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Write CSV header
            writer.write("Fee ID,Property ID,Fee Type,Amount,Is Paid,Payment Method,Pay Date,Created At");
            writer.newLine();

            // Write data rows
            for (Fee fee : fees) {
                StringBuilder line = new StringBuilder();
                line.append(fee.getfId()).append(",");
                line.append(fee.getpId()).append(",");
                line.append(csvEscape(fee.getFeeType())).append(",");
                line.append(fee.getAmount()).append(",");
                line.append(fee.getIsPaid() == 1 ? "已缴" : "未缴").append(",");
                line.append(csvEscape(fee.getPaymentMethod())).append(",");
                line.append(fee.getPayDate() != null ? fee.getPayDate().format(DATE_FORMATTER) : "").append(",");
                line.append(fee.getCreatedAt() != null ? fee.getCreatedAt().format(DATE_FORMATTER) : "");

                writer.write(line.toString());
                writer.newLine();
            }

            System.out.println("✅ 账单信息已导出至: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("❌ 导出账单信息失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * Export arrears list to CSV
     *
     * @param arrearsList List of arrears information (maps)
     * @param filePath    Output file path
     * @return true if successful
     */
    public static boolean exportArrears(List<Map<String, Object>> arrearsList, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Write CSV header
            writer.write("Fee ID,Property ID,Building,Unit,Room,Owner Name,Owner Phone,Fee Type,Amount,Created At");
            writer.newLine();

            // Write data rows
            for (Map<String, Object> arrears : arrearsList) {
                StringBuilder line = new StringBuilder();
                line.append(arrears.get("fee_id")).append(",");
                line.append(arrears.get("property_id")).append(",");
                line.append(csvEscape(String.valueOf(arrears.get("building_no")))).append(",");
                line.append(csvEscape(String.valueOf(arrears.get("unit_no")))).append(",");
                line.append(csvEscape(String.valueOf(arrears.get("room_no")))).append(",");
                line.append(csvEscape(String.valueOf(arrears.get("owner_name")))).append(",");
                line.append(csvEscape(String.valueOf(arrears.get("owner_phone")))).append(",");
                line.append(csvEscape(String.valueOf(arrears.get("fee_type")))).append(",");
                line.append(arrears.get("amount")).append(",");
                Object createdAt = arrears.get("created_at");
                line.append(createdAt != null ? createdAt.toString() : "");

                writer.write(line.toString());
                writer.newLine();
            }

            System.out.println("✅ 欠费名单已导出至: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("❌ 导出欠费名单失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * Escape CSV special characters
     *
     * @param value String value
     * @return Escaped value
     */
    private static String csvEscape(String value) {
        if (value == null) {
            return "";
        }

        // If contains comma, quote, or newline, wrap in quotes and escape internal
        // quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}

package com.mycropdiary.api.util;

// [AI_CHANGE] Root cause: Người dùng yêu cầu email gửi đi chuyên nghiệp, bắt mắt theo nhận diện nông nghiệp VietGAP
// [AI_CHANGE] Mechanism: Xây dựng bộ HTML Email Template tương thích đa nền tảng (Gmail, Outlook, Apple Mail),
//             sử dụng bảng (tables) và inline CSS với bảng màu Emerald Green (#065f46, #10b981), hộp OTP nổi bật
public final class EmailTemplateBuilder {

    private EmailTemplateBuilder() {
        // Utility class
    }

    /**
     * Tạo nội dung HTML cho email xác thực tài khoản khi đăng ký (UC-04).
     */
    public static String buildRegistrationOtpEmail(String fullName, String otp, long expirationMinutes) {
        String title = "🔐 Xác thực tài khoản của bạn";
        String intro = "Cảm ơn bạn đã lựa chọn đăng ký tài khoản trên nền tảng <strong>MyCropDiary</strong>. "
                + "Để hoàn tất việc kích hoạt tài khoản và bắt đầu ghi chép nhật ký canh tác chuẩn VietGAP, "
                + "vui lòng nhập mã xác thực OTP dưới đây vào ứng dụng:";
        String securityWarning = "Tuyệt đối không chia sẻ mã này cho bất kỳ ai, kể cả nhân viên kỹ thuật MyCropDiary. "
                + "Nếu bạn không thực hiện yêu cầu đăng ký này, vui lòng bỏ qua email, tài khoản của bạn sẽ không được kích hoạt.";

        return buildOtpTemplate(fullName, title, intro, otp, expirationMinutes, securityWarning);
    }

    /**
     * Tạo nội dung HTML cho email đặt lại mật khẩu khi quên mật khẩu (UC-06).
     */
    public static String buildPasswordResetOtpEmail(String fullName, String otp, long expirationMinutes) {
        String title = "🔑 Yêu cầu đặt lại mật khẩu";
        String intro = "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản MyCropDiary gắn với email này. "
                + "Vui lòng sử dụng mã OTP dưới đây để tiến hành thiết lập mật khẩu mới:";
        String securityWarning = "Nếu bạn không yêu cầu đặt lại mật khẩu, rất có thể ai đó đã nhập nhầm địa chỉ email của bạn. "
                + "Vui lòng không cung cấp mã cho người khác; mật khẩu hiện tại của bạn vẫn được bảo mật an toàn.";

        return buildOtpTemplate(fullName, title, intro, otp, expirationMinutes, securityWarning);
    }

    /**
     * Tạo nội dung HTML cho email thông báo Admin DUYỆT đơn đăng ký trang trại (UC-39).
     */
    public static String buildFarmApprovedEmail(String fullName, String farmName, String farmCode) {
        return String.format(
                """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Đơn đăng ký trang trại đã được duyệt - MyCropDiary</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #1e293b;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color: #f1f5f9; padding: 40px 10px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width: 580px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05); border: 1px solid #e2e8f0;">
                          <!-- Header -->
                          <tr>
                            <td style="background: linear-gradient(135deg, #065f46 0%%, #059669 50%%, #10b981 100%%); padding: 32px 30px; text-align: center;">
                              <div style="display: inline-block; background: rgba(255, 255, 255, 0.2); border-radius: 50px; padding: 6px 16px; margin-bottom: 12px; border: 1px solid rgba(255, 255, 255, 0.3);">
                                <span style="color: #ffffff; font-size: 13px; font-weight: 600; text-transform: uppercase;">
                                  🌱 Nền Tảng Nông Nghiệp VietGAP
                                </span>
                              </div>
                              <h1 style="color: #ffffff; margin: 0; font-size: 26px; font-weight: 800;">MyCropDiary</h1>
                              <p style="color: #a7f3d0; margin: 6px 0 0 0; font-size: 14px;">Hệ thống Nhật ký Canh tác &amp; Quản lý Chi phí Thông minh</p>
                            </td>
                          </tr>
                          <!-- Content -->
                          <tr>
                            <td style="padding: 36px 32px 28px 32px;">
                              <div style="text-align: center; margin-bottom: 20px;">
                                <span style="font-size: 48px;">🎉</span>
                                <h2 style="color: #065f46; margin: 10px 0 0 0; font-size: 22px; font-weight: 800;">Đơn Đăng Ký Trang Trại Đã Được Duyệt!</h2>
                              </div>
                              <p style="color: #334155; font-size: 15px; margin: 0 0 16px 0;">
                                Kính gửi <strong style="color: #065f46;">%s</strong>,
                              </p>
                              <p style="color: #475569; font-size: 15px; margin: 0 0 20px 0; line-height: 1.6;">
                                Ban Quản trị hệ thống <strong>MyCropDiary</strong> xin trân trọng thông báo: Đơn đăng ký mở trang trại của bạn đã được kiểm duyệt và <strong>CHẤP THUẬN</strong> thành công.
                              </p>
                              <!-- Farm Info Card -->
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 12px; margin: 20px 0;">
                                <tr>
                                  <td style="padding: 20px;">
                                    <div style="font-size: 13px; color: #047857; font-weight: 700; text-transform: uppercase; margin-bottom: 8px;">Thông tin trang trại của bạn:</div>
                                    <div style="font-size: 18px; font-weight: 800; color: #065f46; margin-bottom: 4px;">%s</div>
                                    <div style="font-size: 14px; color: #059669;">Mã định danh trang trại: <strong style="font-family: monospace; background: #ffffff; padding: 2px 8px; border-radius: 4px; border: 1px solid #a7f3d0;">%s</strong></div>
                                    <div style="font-size: 13px; color: #065f46; margin-top: 8px;">Vai trò của bạn: <strong>Chủ trang trại (Farm OWNER)</strong></div>
                                  </td>
                                </tr>
                              </table>
                              <p style="color: #475569; font-size: 14px; margin: 20px 0 0 0; line-height: 1.6;">
                                Bây giờ bạn đã có toàn quyền truy cập trang trại để tạo các vùng sản xuất, phân lô đất và khởi tạo mùa vụ canh tác theo chuẩn VietGAP.
                              </p>
                            </td>
                          </tr>
                          <!-- Footer -->
                          %s
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """,
                escapeHtml(fullName),
                escapeHtml(farmName),
                escapeHtml(farmCode),
                buildEmailFooterHtml()
        );
    }

    /**
     * Tạo nội dung HTML cho email thông báo Admin TỪ CHỐI đơn đăng ký trang trại (UC-39).
     */
    public static String buildFarmRejectedEmail(String fullName, String farmName, String rejectionReason) {
        return String.format(
                """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Kết quả xét duyệt đơn đăng ký trang trại - MyCropDiary</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #1e293b;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color: #f1f5f9; padding: 40px 10px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width: 580px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05); border: 1px solid #e2e8f0;">
                          <!-- Header -->
                          <tr>
                            <td style="background: linear-gradient(135deg, #1e293b 0%%, #334155 100%%); padding: 32px 30px; text-align: center;">
                              <div style="display: inline-block; background: rgba(255, 255, 255, 0.2); border-radius: 50px; padding: 6px 16px; margin-bottom: 12px; border: 1px solid rgba(255, 255, 255, 0.3);">
                                <span style="color: #ffffff; font-size: 13px; font-weight: 600; text-transform: uppercase;">
                                  🌱 Thông Báo Từ Ban Quản Trị
                                </span>
                              </div>
                              <h1 style="color: #ffffff; margin: 0; font-size: 26px; font-weight: 800;">MyCropDiary</h1>
                              <p style="color: #cbd5e1; margin: 6px 0 0 0; font-size: 14px;">Hệ thống Nhật ký Canh tác &amp; Quản lý Chi phí Thông minh</p>
                            </td>
                          </tr>
                          <!-- Content -->
                          <tr>
                            <td style="padding: 36px 32px 28px 32px;">
                              <h2 style="color: #b91c1c; margin: 0 0 16px 0; font-size: 20px; font-weight: 700;">Thông Báo Về Đơn Đăng Ký Trang Trại</h2>
                              <p style="color: #334155; font-size: 15px; margin: 0 0 16px 0;">
                                Kính gửi <strong style="color: #0f172a;">%s</strong>,
                              </p>
                              <p style="color: #475569; font-size: 15px; margin: 0 0 20px 0; line-height: 1.6;">
                                Ban Quản trị hệ thống <strong>MyCropDiary</strong> đã tiến hành xem xét đơn đăng ký mở trang trại <strong>%s</strong> của bạn.
                                Rất tiếc, đơn đăng ký hiện chưa đủ điều kiện để phê duyệt.
                              </p>
                              <!-- Reason Box -->
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color: #fef2f2; border-left: 4px solid #ef4444; border-radius: 8px; margin: 20px 0;">
                                <tr>
                                  <td style="padding: 16px 20px;">
                                    <div style="font-size: 13px; font-weight: 700; color: #991b1b; margin-bottom: 4px;">LÝ DO TỪ CHỐI TỪ ADMIN:</div>
                                    <div style="font-size: 14px; color: #7f1d1d; line-height: 1.5;">%s</div>
                                  </td>
                                </tr>
                              </table>
                              <p style="color: #64748b; font-size: 14px; margin: 20px 0 0 0; line-height: 1.6;">
                                Bạn có thể chỉnh sửa thông tin theo hướng dẫn nêu trên và gửi lại đơn đăng ký mới bất cứ lúc nào.
                              </p>
                            </td>
                          </tr>
                          <!-- Footer -->
                          %s
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """,
                escapeHtml(fullName),
                escapeHtml(farmName),
                escapeHtml(rejectionReason),
                buildEmailFooterHtml()
        );
    }

    // ==================== Template dùng chung cho OTP ====================
    private static String buildOtpTemplate(String fullName, String title, String intro,
                                           String otp, long expirationMinutes, String securityWarning) {
        return String.format(
                """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s - MyCropDiary</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased; line-height: 1.6; color: #1e293b;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color: #f1f5f9; padding: 40px 10px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width: 580px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.01); border: 1px solid #e2e8f0;">
                          
                          <!-- Brand Header Banner -->
                          <tr>
                            <td style="background: linear-gradient(135deg, #065f46 0%%, #059669 50%%, #10b981 100%%); padding: 32px 30px; text-align: center;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td align="center">
                                    <div style="display: inline-block; background: rgba(255, 255, 255, 0.2); border-radius: 50px; padding: 6px 16px; margin-bottom: 12px; border: 1px solid rgba(255, 255, 255, 0.3);">
                                      <span style="color: #ffffff; font-size: 13px; font-weight: 600; letter-spacing: 0.5px; text-transform: uppercase;">
                                        🌱 Nền Tảng Nông Nghiệp VietGAP
                                      </span>
                                    </div>
                                    <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 800; letter-spacing: -0.5px;">
                                      MyCropDiary
                                    </h1>
                                    <p style="color: #a7f3d0; margin: 6px 0 0 0; font-size: 14px; font-weight: 400;">
                                      Hệ thống Nhật ký Canh tác &amp; Quản lý Chi phí Thông minh
                                    </p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Main Content Body -->
                          <tr>
                            <td style="padding: 36px 32px 28px 32px;">
                              <h2 style="color: #0f172a; margin: 0 0 16px 0; font-size: 20px; font-weight: 700;">
                                %s
                              </h2>

                              <p style="color: #334155; font-size: 15px; margin: 0 0 16px 0;">
                                Xin chào <strong style="color: #065f46;">%s</strong>,
                              </p>

                              <p style="color: #475569; font-size: 15px; margin: 0 0 24px 0; line-height: 1.6;">
                                %s
                              </p>

                              <!-- Highlighted OTP Card -->
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin: 24px 0;">
                                <tr>
                                  <td align="center" style="background-color: #ecfdf5; border: 2px dashed #34d399; border-radius: 12px; padding: 24px 16px; text-align: center;">
                                    <div style="font-size: 12px; font-weight: 700; color: #047857; text-transform: uppercase; letter-spacing: 1.5px; margin-bottom: 8px;">
                                      MÃ XÁC THỰC CỦA BẠN (OTP)
                                    </div>
                                    <div style="font-size: 38px; font-weight: 800; letter-spacing: 12px; color: #065f46; font-family: 'Courier New', Courier, monospace; margin: 6px 0 10px 12px;">
                                      %s
                                    </div>
                                    <div style="display: inline-block; background-color: #d1fae5; color: #065f46; font-size: 13px; font-weight: 600; padding: 4px 12px; border-radius: 20px;">
                                      ⏱ Hiệu lực trong vòng %d phút
                                    </div>
                                  </td>
                                </tr>
                              </table>

                              <!-- Security Warning Box -->
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color: #fffbeb; border-left: 4px solid #f59e0b; border-radius: 8px; margin: 24px 0;">
                                <tr>
                                  <td style="padding: 14px 16px;">
                                    <div style="font-size: 13px; font-weight: 700; color: #92400e; margin-bottom: 4px;">
                                      🛡️ Khuyến nghị bảo mật
                                    </div>
                                    <p style="margin: 0; font-size: 13px; color: #78350f; line-height: 1.5;">
                                      %s
                                    </p>
                                  </td>
                                </tr>
                              </table>

                              <p style="color: #64748b; font-size: 14px; margin: 20px 0 0 0;">
                                Nếu bạn không thực hiện yêu cầu này, bạn có thể an tâm bỏ qua email này.
                              </p>
                            </td>
                          </tr>

                          <!-- Divider & Footer -->
                          %s
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """,
                escapeHtml(title),
                escapeHtml(title),
                escapeHtml(fullName),
                intro,
                escapeHtml(otp),
                expirationMinutes,
                securityWarning,
                buildEmailFooterHtml()
        );
    }

    private static String buildEmailFooterHtml() {
        return """
          <tr>
            <td style="padding: 0 32px;">
              <hr style="border: 0; border-top: 1px solid #e2e8f0; margin: 0;">
            </td>
          </tr>
          <tr>
            <td style="padding: 24px 32px 32px 32px; background-color: #fafafa; text-align: center;">
              <p style="margin: 0 0 6px 0; font-size: 13px; font-weight: 600; color: #047857;">
                🌾 MyCropDiary - Quản lý chi phí &amp; Nhật ký nông trại VietGAP
              </p>
              <p style="margin: 0 0 8px 0; font-size: 12px; color: #64748b;">
                Mã đồ án: SEP490_05 &bull; Đại học FPT Cần Thơ &bull; Niên khóa 2026
              </p>
              <p style="margin: 0; font-size: 11px; color: #94a3b8; line-height: 1.4;">
                Đây là email tự động gửi từ máy chủ MyCropDiary. Vui lòng không trả lời trực tiếp email này.
              </p>
            </td>
          </tr>
        """;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

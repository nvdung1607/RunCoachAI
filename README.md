# 🏃‍♂️ RunCoach AI - Huấn Luyện Viên Chạy Bộ Cá Nhân Hóa

RunCoach AI là ứng dụng Android thông minh giúp thiết kế và quản lý giáo án chạy bộ cá nhân hóa dựa trên các phương pháp huấn luyện thể thao chuyên nghiệp đạt chuẩn quốc tế. Ứng dụng hỗ trợ người dùng từ người mới bắt đầu (Beginner) đến chân chạy phong trào nâng cao (Advanced) chuẩn bị tốt nhất cho ngày đua (Race Day) của mình.

---

## 🌟 Tính Năng Nổi Bật

### 1. Thuật Toán Sinh Giáo Án Chuẩn Quốc Tế (`PlanGenerator` & `VdotCalculator`)
*   **Hệ thống 5 vùng tốc độ VDOT (VDOT Zones):** Tự động cá nhân hóa vùng Pace tập luyện của bạn dựa trên chỉ số VDOT (theo công thức của Jack Daniels):
    *   **E (Easy Pace):** Chạy nhẹ nhàng phục hồi và xây dựng nền tảng tim mạch.
    *   **L (Long Run Pace):** Chạy dài tích lũy sức bền.
    *   **M (Marathon Pace):** Tốc độ mô phỏng ngày đua.
    *   **T (Tempo/Threshold Pace):** Nâng cao ngưỡng yếm khí (lactic threshold).
    *   **I (Interval Pace):** Tập biến tốc phát triển dung tích oxy tối đa (VO2 Max).
*   **Phân Kỳ Tập Luyện 4 Giai Đoạn (Periodization):** Giáo án không tăng trưởng tịnh tiến nhàm chán mà chia thành:
    *   **Base Phase (Tích lũy nền tảng):** Tập trung chạy Easy & Long Run xây dựng sức bền cơ sở.
    *   **Build Phase (Phát triển):** Tăng khối lượng tuần và bổ sung bài chạy chất lượng cao (Tempo, Interval).
    *   **Peak Phase (Đạt đỉnh):** Khối lượng tuần lớn nhất, bài Long Run chuyển sang mô phỏng tốc độ Marathon (M-Pace).
    *   **Taper Phase (Giảm tải dưỡng sức):** Giảm quãng đường chạy để cơ thể phục hồi tối đa trước ngày đua nhưng vẫn duy trì độ nhạy cơ bắp.
*   **Chu Kỳ Phục Hồi (Deload Weeks):** Cứ mỗi 4 tuần tập luyện, quãng đường chạy dài sẽ tự động giảm xuống còn 75% để tránh quá tải và chấn thương.
*   **Tỷ Lệ Vàng 80/20:** Sắp xếp lịch chạy đảm bảo 80% thời lượng/quãng đường ở cường độ thấp (Easy/Long) và chỉ 20% dành cho các bài tập chất lượng cao (Tempo/Interval).
*   **Giáo Án An Toàn & Xây Dựng Thói Quen (New):** Thiết lập cự ly 3 tuần đầu tiên siêu dễ dàng cho cấp độ `BEGINNER` để tạo thói quen và thích nghi cơ xương khớp mà không bị áp lực Pace, kết hợp hướng dẫn xen kẽ chạy/đi bộ (Run-Walk). Đồng thời áp dụng giới hạn tăng trưởng cự ly tuần (`maxStep`) cực kỳ nghiêm ngặt.
*   **Đánh Giá Tính Khả Thi Mục Tiêu (New):** Hệ thống `PlanFeasibilityChecker` tự động phân tích thể lực thực tế (3km test) so với số tuần chuẩn bị và cự ly mục tiêu để đưa ra cảnh báo chấn thương trực quan và hướng dẫn người dùng điều chỉnh ngày đua/cự ly phù hợp.

### 2. Đồng Bộ Hóa Qua Health Connect (Google)
*   Tích hợp trực tiếp với thư viện **Health Connect** của Android.
*   Dễ dàng đồng bộ các bài chạy thực tế từ các nguồn/ứng dụng khác như Strava, Garmin, Google Fit, Samsung Health, Wahoo,... về ứng dụng RunCoach AI.
*   Cung cấp nút **Đồng bộ** nhanh cùng hệ thống Toast phản hồi trạng thái rõ ràng (Thành công, Không tìm thấy dữ liệu, Quãng đường không đủ,...).

### 3. Tiện Ích Màn Hình Chính (App Widget)
*   Widget đếm ngược số ngày tới Race Day trực quan ngay ngoài màn hình chính.
*   Hiển thị thông tin **Bài tập hôm nay** (Loại bài tập, quãng đường mục tiêu và mô tả chi tiết).
*   Tích hợp nút làm mới nhanh (🔄) giúp cập nhật dữ liệu tức thì mà không cần mở ứng dụng.
*   Hỗ trợ hoàn hảo **Light/Dark Mode** của hệ thống với thiết kế bán trong suốt (semi-transparent) cao cấp.

### 4. Giao Diện Người Dùng Hiện Đại (Jetpack Compose)
*   **Dashboard tương tác:** Các thẻ chỉ số (Race Day, VDOT, Vùng Pace, Khối lượng tuần) đều có thể nhấn vào để mở popup giải thích chi tiết ý nghĩa khoa học.
*   **Quản lý lịch tập (Calendar Screen):** Lịch tháng trực quan hiển thị trạng thái hoàn thành bài tập, tô màu phân biệt ngày hôm nay và các bài chạy thực tế.
*   **Lịch sử tập luyện (History Screen):** Xem lại toàn bộ danh sách bài chạy đã thực hiện, tính toán Pace thực tế và hiển thị nguồn đồng bộ.
*   **Hỗ trợ đa dạng cấu hình:** Cho phép chọn ngày chạy dài ưa thích (Thứ Bảy / Chủ Nhật) và số buổi tập mỗi tuần (2 - 5 buổi).

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

*   **Ngôn ngữ chính:** Kotlin
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Quản lý dữ liệu:** Room Database (Lưu trữ lịch tập và thông số cấu hình cục bộ)
*   **Xử lý nền:** WorkManager (Hỗ trợ chạy đồng bộ Health Connect định kỳ)
*   **Tích hợp dữ liệu:** Android Health Connect SDK
*   **Widget:** Android RemoteViews & AppWidgetProvider
*   **Dependency Injection:** Manual DI / Factory Pattern
*   **Logging:** Tích hợp `AppLogger` tùy chỉnh hiển thị chi tiết StackTrace lớp/phương thức/dòng mã gọi log.

---

## 🚀 Hướng Dẫn Cài Đặt & Cấu Hình

### Yêu cầu hệ thống
*   Thiết bị chạy hệ điều hành **Android 8.0 (API 26) trở lên**.
*   Tính năng Health Connect yêu cầu thiết bị chạy **Android 12+** hoặc đã cài đặt ứng dụng Health Connect từ Google Play Store (đối với Android cũ hơn).

### Các bước thiết lập ban đầu (Onboarding)
1.  **Chọn ngày đua (Race Date):** Chọn ngày diễn ra giải chạy của bạn (phải cách ngày hiện tại tối thiểu 4 tuần).
2.  **Chọn cự ly mục tiêu:** 5km, 10km, 21km (Half Marathon) hoặc 42km (Full Marathon).
3.  **Chọn số buổi tập tuần:** Tối đa số buổi bạn có thể tập trong tuần (từ 2 đến 5 buổi).
4.  **Cấp quyền:** Cho phép ứng dụng gửi thông báo nhắc nhở và kết nối đọc dữ liệu từ **Health Connect**.
5.  **Chạy thử nghiệm kiểm tra thể lực (Test Run):** Thực hiện một bài chạy thử 1.5km - 3km để hệ thống đo đạc Pace, tự động tính toán chỉ số VDOT ban đầu và thiết lập vùng Pace riêng cho bạn.

---

## 📖 Hướng Dẫn Sử Dụng Hằng Ngày

### Theo dõi và Thực hiện bài tập
*   Mở ứng dụng, tại thẻ **Bài tập hôm nay**, bạn sẽ thấy chi tiết bài tập được chỉ định (Ví dụ: *Chạy Easy phục hồi (6.0km)* cùng hướng dẫn nhịp thở, chiến thuật chạy).
*   Sau khi chạy xong, bạn có 2 cách để ghi nhận bài tập:
    1.  **Nhấn "Đồng bộ":** Nếu bạn ghi lại bài chạy qua đồng hồ Garmin/Coros hoặc ứng dụng Strava đã liên kết với Health Connect, hệ thống sẽ tự động quét và điền dữ liệu thực tế.
    2.  **Nhấn "Hoàn thành" (Thủ công):** Tự điền cự ly, thời gian hoàn thành và đánh giá mức độ gắng sức (RPE - Rate of Perceived Exertion) từ 1 đến 10.

### Xem Lịch Trình & Biểu Đồ
*   **Tab Giáo án (Plan):** Xem toàn bộ kế hoạch tập luyện chi tiết của các tuần tiếp theo.
*   **Tab Lịch tháng (Calendar):** Theo dõi tổng quan các ngày tập/nghỉ trong tháng. Các ngày có bài tập hoàn thành sẽ hiển thị chấm màu xanh lá.
*   **Tab Lịch sử (History):** Xem danh sách tất cả các bài chạy đã thực hiện kèm theo thông số Pace thực tế trung bình.
*   **Biểu đồ khối lượng (Weekly Volume Chart):** Biểu đồ cột ở cuối trang chủ hiển thị tổng quãng đường chạy tích lũy theo từng tuần giúp bạn giám sát sự tiến bộ.

---

## 🔒 An Toàn & Bảo Mật

*   **Offline First:** Mọi dữ liệu cá nhân, lịch tập và lịch sử chạy bộ của bạn đều được lưu trữ hoàn toàn cục bộ trên thiết bị thông qua cơ sở dữ liệu Room bảo mật.
*   **Health Connect Data:** Ứng dụng chỉ yêu cầu quyền đọc (`READ_EXERCISE`, `READ_DISTANCE`) từ Health Connect để phục vụ việc đồng bộ bài chạy thực tế và tuyệt đối không chia sẻ hay tải dữ liệu của bạn lên bất kỳ máy chủ bên thứ ba nào.

---

## 📝 Thông Tin Phiên Bản

*   **Phiên bản hiện tại:** v1.3
    *   *Nâng cấp v1.3:*
        *   **Bắt đầu bài tập hôm nay:** Buổi tập đầu tiên luôn được lên lịch vào ngày bắt đầu giáo án (hôm nay) thay vì là ngày nghỉ (REST).
        *   **Khởi đầu nhẹ nhàng:** Bắt buộc 3 buổi tập tích lũy đầu tiên của giáo án luôn là bài chạy nhẹ nhàng (EASY run), tránh gặp bài chạy dài (LONG run) hay bài chất lượng (QUALITY run) quá sớm ở tuần đầu tiên.
        *   **Kiểm soát số buổi tập:** Đảm bảo tổng số buổi tập trong tuần đầu tiên không vượt quá giới hạn tối đa `maxSessionsPerWeek` đã chọn.
    *   *Nâng cấp v1.2:*
        *   **Cơ chế giáo án an toàn:** Tích hợp giới hạn tăng cự ly tuần (`maxStep`), giai đoạn 3 tuần đầu tạo thói quen cực kỳ nhẹ nhàng cho người mới bắt đầu và chỉ dẫn xen kẽ chạy/đi bộ (Run-Walk).
        *   **Đánh giá tính khả thi:** Thêm `PlanFeasibilityChecker` đánh giá độ an toàn của mục tiêu và hiển thị hộp thoại cảnh báo Material 3 trên `TestRunScreen`, cho phép người dùng quay lại Onboarding để điều chỉnh mục tiêu hoặc chọn tiếp tục.
    *   *Nâng cấp v1.1:* Tối ưu hóa giao diện Scaffold triệt tiêu khoảng trắng thừa ở chân trang, cải tiến Widget chống crash trên các dòng máy cao cấp (Samsung S23 Ultra - Android 14), và sửa đổi hiển thị phiên bản ở chân màn hình Onboarding và Dashboard.

---
*Chúc bạn có những bước chạy khỏe mạnh và đạt thành tích tốt nhất trong ngày đua!* 🏁🏆

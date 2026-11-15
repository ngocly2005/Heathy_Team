# Cảnh báo mục tiêu sức khỏe — 15/11/2026

Các cảnh báo vàng xuất hiện **trước khi lưu** ở màn Thiết lập mục tiêu hoặc khi
lượng nước ghi nhận tăng cao. Chúng giúp người dùng kiểm tra lại mục tiêu, không
tự chẩn đoán và không tự thay thế lời khuyên của chuyên gia. Giới hạn định dạng
và tổng macro 100% vẫn chặn nút Lưu như trước.

| Mục | Khi hiện cảnh báo | Cách hiểu |
|---|---|---|
| Chất đạm / đường bột / chất béo | Ngoài lần lượt 10–35% / 45–65% / 20–35% năng lượng | Khoảng phân bố đa lượng tham khảo cho người từ 19 tuổi; dưới 19 tuổi cần đánh giá riêng. |
| Cân nặng mong muốn | Giảm trên 4 kg so với hiện tại; hoặc BMI mục tiêu dưới 18,5 với người từ 20 tuổi | Không có ngày đích trong dữ liệu, nên app ước tính **thời gian tối thiểu** theo khoảng 0,5–1 kg/tuần, không coi mục tiêu dài hạn là sai. BMI chỉ dùng sàng lọc. |
| Calo nhập thủ công | Dưới giá trị lớn hơn giữa BMR ước tính và mốc rà soát 1.200 kcal/ngày (nữ), 1.500 kcal/ngày (nam); hoặc cao hơn TDEE ước tính trên 500 kcal/ngày | Đây là **mốc nhắc kiểm tra của ứng dụng**, không phải ngưỡng tuyệt đối áp dụng cho mọi người. Chế độ có giám sát y tế có thể khác. Không áp dụng trực tiếp mốc người lớn cho người dưới 19 tuổi. |
| Mục tiêu nước | Trên 4,5 lít/ngày hoặc hơn 150% mức tự tính theo hồ sơ | 4,5 lít là giới hạn **tự tính của ứng dụng**, không phải giới hạn độc hại do y khoa xác lập. |
| Nước đã ghi trong ngày | Vượt hơn 10% mục tiêu, hoặc trên 4,5 lít | Vượt mục tiêu không đồng nghĩa ngộ độc nước; trên 4,5 lít app nhắc không uống dồn quá nhanh. BE chỉ lưu ngày, không lưu giờ từng ly; app không thể xác định tốc độ uống. |

BE vẫn kiểm tra phạm vi số và lưu mục tiêu theo hợp đồng API hiện có. Cảnh báo
chỉ trình bày ở Android, không thay đổi cấu trúc database hay migration. Nếu một
nguồn dữ liệu đọc tạm thời lỗi, Android thử lại tối đa hai lần với khoảng chờ
ngắn; các yêu cầu ghi không tự gửi lại sau lỗi không rõ kết quả.

## Nguồn tham khảo

- [National Academies / Health Canada: AMDR cho người lớn](https://www.canada.ca/en/health-canada/services/food-nutrition/healthy-eating/dietary-reference-intakes/tables/reference-values-macronutrients.html).
- [CDC: giảm khoảng 1–2 pound mỗi tuần](https://www.cdc.gov/healthy-weight-growth/losing-weight/index.html).
- [CDC: BMI người lớn là chỉ số sàng lọc](https://www.cdc.gov/bmi/adult-calculator/bmi-categories.html).
- [NIDDK: dưới 1.200 kcal/ngày không được khuyến nghị trong chương trình tham khảo](https://www.niddk.nih.gov/health-information/diabetes/overview/preventing-type-2-diabetes/game-plan).
- [NHLBI: khẩu phần calo cần cá nhân hóa, mức giảm thường 500–1.000 kcal/ngày](https://www.nhlbi.nih.gov/sites/default/files/publications/05-5213.pdf).
- [National Academies: nước không có UL chung; uống dồn vượt khả năng thải có thể gây hạ natri máu](https://www.nationalacademies.org/read/10925/chapter/6).

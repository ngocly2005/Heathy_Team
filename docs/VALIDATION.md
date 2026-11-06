# Kiểm chứng tích hợp 2026-11-05

## Đã chạy

- Backend: 9 regression tests HTTP thành công trong venv mới cài đúng dependencies
  repo; SQLite trong bộ nhớ, Google/Groq được mock.
- Android: build `app-debug.apk`, 4 unit tests thành công (1 có sẵn, 3 mới).
  `lintDebug` thành công, không có Error; vẫn còn Warning/Hint trong báo cáo.
- React Admin: TypeScript/production build và 2 test URL/filter thành công.
  Npm audit không còn vulnerability tại thời điểm kiểm tra.
- Alembic: một head `20261105_0011`, sinh SQL PostgreSQL offline; không upgrade DB thật.
- Bộ FE theo sprint: checksum, patch và phạm vi Git được kiểm tra bằng script
  trong `trien_khai_git_fe`; kết quả ở `verification.json` của bộ đó.

BE tests kiểm tra quyền/ownership, Google exchange, refresh/revoke, hồ sơ/mục tiêu,
snapshot khi đổi catalog, food/activity/water, saved meals, weight/diary/calendar,
phân tích ảnh không ghi rồi xác nhận giá trị đã sửa, AI history và ngày UTC+7.

## Chưa chạy và giới hạn

- Google thật, ảnh/chat Groq thật: cần Client ID, SHA-1, key và model giáo viên.
- PostgreSQL upgrade, Docker và bootstrap Admin thật: giáo viên tự chạy theo README BE.
- Chưa kiểm tra UI/camera trên thiết bị hoặc emulator với tài khoản thật.
- Đã thử skill browser và Chrome nhưng không kết nối được bridge/debugger.
  Không nhận rằng đã xem screenshot hoặc thao tác UI React bằng trình duyệt.
- Vite có cảnh báo bundle lớn; lint Android và thư viện test BE còn cảnh báo.
- Bản cuối được build/test; không nhận mọi checkpoint đã qua kiểm tra trên thiết bị.

Trước khi ghi Done trong báo cáo lớp, kiểm tra Google login, lưu/đọc lại trên BE
thật, đổi tài khoản/ngày, camera/AI thật, Admin CRUD/charts/logs và mất kết nối khi ghi.

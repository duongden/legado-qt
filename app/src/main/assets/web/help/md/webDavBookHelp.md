# Hướng dẫn sử dụng đơn giản sách WebDav

> Trang trợ giúp này sẽ bật lên khi vào lần đầu tiên, sau đó sẽ không xuất hiện nữa, nếu muốn xem, vui lòng nhấp vào góc trên bên phải “**⁝**” > Trợ giúp để xem trang này.

Mặc dù Legado chủ yếu là công cụ để đọc tiểu thuyết mạng, nhưng để thuận tiện cho các bạn đọc sách, cũng cung cấp một số hỗ trợ đơn giản cho việc đọc sách cục bộ (epub, txt)

Nhưng một vấn đề nan giải khi đọc sách cục bộ là làm thế nào để đồng bộ tiến độ đọc cũng như sách trên nhiều thiết bị, giả sử sau khi đổi thiết bị, sách cục bộ trên thiết bị cũ cũng phải nhập lại thủ công, không tiện lắm.

Bản thân Legado không có máy chủ riêng, không có khả năng lưu trữ máy chủ như Duokan, WeChat Read, nhưng Legado hỗ trợ sao lưu WebDav, vậy chúng ta cũng có thể tận dụng WebDav để đồng bộ sách.

### Điều kiện tiên quyết
1. Cấu hình vị trí lưu trữ sách (Vị trí lưu trữ tải xuống sách WebDav): Lần lượt nhấp vào Của tôi/Cài đặt khác/Vị trí lưu trữ sách, chọn vị trí lưu sách là được.

2. Cấu hình sao lưu WebDav (Vị trí lưu sách WebDav): Của tôi/Sao lưu và khôi phục/Cài đặt WebDav. Ở đây cần cấu hình địa chỉ máy chủ, tài khoản, mật khẩu sao lưu WebDav. Phương án cấu hình chi tiết không nói lại ở đây, vui lòng xem bài viết này: [Đăng ký và cấu hình Jianguoyun · Yuque (yuque.com)](https://www.yuque.com/legado/wiki/fkx510) hoặc nhấp vào nút trợ giúp ở góc trên bên phải trang đó, xem phương pháp cấu hình.

### Tải sách lên WebDav

Sau khi cấu hình WebDav xong, vào trang sách WebDav từ giao diện chính không hiển thị sách nào cả, điều này rất bình thường, vì trên máy chủ WebDav của chúng ta chưa có bất kỳ cuốn sách nào.

Hiện tại có ba cách để tải sách lên WebDav:

1. Tải lên sách cục bộ đã nhập trên App.

   Nhấn giữ sách cục bộ đã nhập để vào chi tiết sách > Góc trên bên phải “**⁝**” tìm **Tải lên WebDav**, nhấp vào, đợi vài giây là có thể tải lên thành công.

2. Tải lên sách mạng đã lưu bộ nhớ đệm trên App.

   Góc trên bên phải giao diện chính nhấp vào Cài đặt thêm > Nhấp vào Bộ nhớ đệm/Xuất, ở góc trên bên phải trang này “**⁝**” tìm **Xuất sang WebDav** và tích chọn. Như vậy khi xuất sách sẽ tự động tải lên một bản sao vào máy chủ WebDav.

3. Sử dụng client Jianguoyun/Client dịch vụ WebDav tự xây dựng để tải lên.

   Đối với đa số người dùng, tải lên bằng App là đủ rồi, nhưng một số người dùng số lượng sách có thể khá lớn, vậy chúng tôi không khuyên bạn tải lên từng cuốn một thông qua App, cách tốt hơn là sử dụng client của dịch vụ WebDav bạn đang sử dụng để tải lên hàng loạt.

   Giả sử chúng ta sử dụng dịch vụ WebDav của Jianguoyun, vào [Trang chủ Jianguoyun](https://www.jianguoyun.com/d/home#/) , tải xuống client nền tảng tương ứng cài đặt và chạy, tìm thư mục mục lục legado/books, đây chính là vị trí lưu trữ sách, bạn có thể tải sách hàng loạt lên thư mục này.

**Bất kể sử dụng cách nào trong các cách trên để tải sách lên, để đảm bảo tải lên không có lỗi, tốt nhất bạn nên vào trang sách WebDav kiểm tra xem có thấy sách đã tải lên hay không sau khi tải sách lên.**

### Tải sách WebDav về máy

Khác với sự đa dạng của các cách tải lên, cách tải sách về máy khá đơn giản.

Duyệt danh sách sách đã tải lên trong **Trang sách WebDav**, tìm cuốn sách mình muốn tải xuống, nhấp nút **Thêm vào kệ sách**, phần mềm sẽ tự động tải cuốn sách đó về máy và thêm vào kệ sách.

### Lưu ý

- Nếu sử dụng dịch vụ WebDav của Jianguoyun, hạn ngạch lưu lượng miễn phí là đủ cho việc đồng bộ cài đặt App v.v. cũng như **số lượng nhỏ sách**. Nhưng nếu là người dùng cần tải lên/tải xuống sách thường xuyên thì lưu lượng có thể không đủ dùng, vui lòng chú ý dung lượng sử dụng cá nhân, tránh vượt quá hạn ngạch ảnh hưởng đến việc đồng bộ cài đặt App.

### Câu hỏi thường gặp

- Vào **Trang sách WebDav** báo "Lỗi lấy sách WebDav webDav chưa được cấu hình".

  > Điều này là do chưa cấu hình dịch vụ đồng bộ WebDav, cấu hình theo phương pháp cấu hình đồng bộ Webdav đã đề cập trong mục Điều kiện tiên quyết ở trên là được.

- Sách cục bộ do thiết bị A tải lên có thể xem được trên thiết bị B không, có thể tự động thêm vào kệ sách không?

  > Nếu thiết bị A và thiết bị B cấu hình cùng một dịch vụ WebDav, thì B có thể nhìn thấy sách do A tải lên trong **Trang sách WebDav**. Tuy nhiên không thể nhìn thấy trực tiếp cuốn sách đó trên kệ sách, điều này có thể sau này sẽ nghĩ phương án để làm, hiện tại bắt buộc phải tự tìm cuốn sách đó trong **Trang sách WebDav** và nhấp thủ công **Thêm vào kệ sách** để nhập mới được.

- Tiến độ đọc/Dấu trang v.v. của sách cục bộ có đồng bộ không?

  > Có thể đồng bộ.
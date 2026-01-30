# [English](English.md) [Tiếng Việt](README.md)

[![icon_android](https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/icon_android.png)](https://play.google.com/store/apps/details?id=io.legado.play.release)
<a href="https://jb.gg/OpenSourceSupport" target="_blank">
<img width="24" height="24" src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg?_gl=1*135yekd*_ga*OTY4Mjg4NDYzLjE2Mzk0NTE3MzQ.*_ga_9J976DJZ68*MTY2OTE2MzM5Ny4xMy4wLjE2NjkxNjMzOTcuNjAuMC4w&_ga=2.257292110.451256242.1669085120-968288463.1639451734" alt="idea"/>
</a>

<div align="center">
<img width="125" height="125" src="https://github.com/gedoor/legado/raw/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="legado"/>  
  
Legado / Đọc sách nguồn mở
<br>
<a href="https://gedoor.github.io" target="_blank">gedoor.github.io</a> / <a href="https://www.legado.top/" target="_blank">legado.top</a>
<br>
Legado là trình đọc tiểu thuyết miễn phí và nguồn mở cho Android.
</div>

[![](https://img.shields.io/badge/-Contents:-696969.svg)](#contents) [![](https://img.shields.io/badge/-Function-F5F5F5.svg)](#Function-Ch%E1%BB%A9c-n%C4%83ng-ch%C3%ADnh-) [![](https://img.shields.io/badge/-Community-F5F5F5.svg)](#Community-C%E1%BB%99ng-%C4%91%E1%BB%93ng-) [![](https://img.shields.io/badge/-API-F5F5F5.svg)](#API-) [![](https://img.shields.io/badge/-Other-F5F5F5.svg)](#Other-Kh%C3%A1c-) [![](https://img.shields.io/badge/-Grateful-F5F5F5.svg)](#Grateful-C%E1%BA%A3m-%C6%A1n-) [![](https://img.shields.io/badge/-Interface-F5F5F5.svg)](#Interface-Giao-di%E1%BB%87n-)

>Người dùng mới?
>
>Phần mềm không cung cấp nội dung, bạn cần tự thêm thủ công, ví dụ như nhập nguồn sách.
>Xem [Tài liệu trợ giúp chính thức](https://www.yuque.com/legado/wiki), có thể câu trả lời bạn cần nằm ở đó.

# Function-Chức năng chính [![](https://img.shields.io/badge/-Function-F5F5F5.svg)](#Function-Ch%E1%BB%A9c-n%C4%83ng-ch%C3%ADnh-)
[English](English.md)

<details><summary>Tiếng Việt</summary>
1. Tùy chỉnh nguồn sách, tự thiết lập quy tắc, thu thập dữ liệu trang web, quy tắc đơn giản dễ hiểu, có hướng dẫn quy tắc trong phần mềm.<br>
2. Tự do chuyển đổi giữa kệ sách dạng danh sách và dạng lưới.<br>
3. Nguồn sách hỗ trợ tìm kiếm và khám phá, tất cả các chức năng tìm sách và đọc sách đều có thể tùy chỉnh, giúp việc tìm sách thuận tiện hơn.<br>
4. Đăng ký nội dung, có thể đăng ký bất kỳ nội dung nào bạn muốn xem, xem những gì bạn muốn xem<br>
5. Hỗ trợ thay thế và làm sạch, loại bỏ quảng cáo và thay thế nội dung rất tiện lợi.<br>
6. Hỗ trợ đọc TXT, EPUB cục bộ, duyệt thủ công, quét thông minh.<br>
7. Hỗ trợ giao diện đọc tùy chỉnh cao, thay đổi phông chữ, màu sắc, nền, khoảng cách dòng, khoảng cách đoạn, in đậm, chuyển đổi giản/phồn, v.v.<br>
8. Hỗ trợ nhiều chế độ lật trang, bao gồm phủ, mô phỏng, trượt, cuộn, v.v.<br>
9. Phần mềm nguồn mở, tối ưu hóa liên tục, không quảng cáo.
</details>

<a href="#readme">
    <img src="https://img.shields.io/badge/-V%E1%BB%81-%C4%91%E1%BA%A7u-orange.svg" alt="#" align="right">
</a>

# Community-Cộng đồng [![](https://img.shields.io/badge/-Community-F5F5F5.svg)](#Community-C%E1%BB%99ng-%C4%91%E1%BB%93ng-)

#### Telegram
[![Telegram-group](https://img.shields.io/badge/Telegram-Nh%C3%B3m-blue)](https://t.me/yueduguanfang) [![Telegram-channel](https://img.shields.io/badge/Telegram-K%C3%AAvn-blue)](https://t.me/legado_channels)

#### Discord
[![Discord](https://img.shields.io/discord/560731361414086666?color=%235865f2&label=Discord)](https://discord.gg/VtUfRyzRXn)

#### Other
https://www.yuque.com/legado/wiki/community

<a href="#readme">
    <img src="https://img.shields.io/badge/-V%E1%BB%81-%C4%91%E1%BA%A7u-orange.svg" alt="#" align="right">
</a>

# API [![](https://img.shields.io/badge/-API-F5F5F5.svg)](#API-)
* Legado 3.0 cung cấp 2 phương thức API: `Phương thức Web` và `Phương thức Content Provider`. Bạn có thể tự gọi theo nhu cầu tại [đây](api.md). 
* Có thể gọi Legado để nhập nhanh qua url, định dạng url: legado://import/{path}?src={url}
* Loại path: bookSource, rssSource, replaceRule, textTocRule, httpTTS, theme, readConfig, dictRule, [addToBookshelf](/app/src/main/java/io/legado/app/ui/association/AddToBookshelfDialog.kt)
* Giải thích loại path: Nguồn sách, Nguồn đăng ký, Quy tắc thay thế, Quy tắc mục lục txt cục bộ, Công cụ đọc trực tuyến, Chủ đề, Cấu hình đọc, Quy tắc từ điển, Thêm vào kệ sách

<a href="#readme">
    <img src="https://img.shields.io/badge/-V%E1%BB%81-%C4%91%E1%BA%A7u-orange.svg" alt="#" align="right">
</a>

# Other-Khác [![](https://img.shields.io/badge/-Other-F5F5F5.svg)](#Other-Kh%C3%A1c-)
##### Tuyên bố miễn trừ trách nhiệm
https://gedoor.github.io/Disclaimer

##### Legado 3.0
* [Quy tắc nguồn sách](https://mgz0227.github.io/The-tutorial-of-Legado/)
* [Nhật ký cập nhật](/app/src/main/assets/updateLog.md)
* [Tài liệu trợ giúp](/app/src/main/assets/web/help/md/appHelp.md)
* [Kệ sách Web](https://github.com/gedoor/legado_web_bookshelf)
* [Biên tập nguồn Web](https://github.com/gedoor/legado_web_source_editor)

<a href="#readme">
    <img src="https://img.shields.io/badge/-V%E1%BB%81-%C4%91%E1%BA%A7u-orange.svg" alt="#" align="right">
</a>

# Grateful-Cảm ơn [![](https://img.shields.io/badge/-Grateful-F5F5F5.svg)](#Grateful-C%E1%BA%A3m-%C6%A1n-)
> * org.jsoup:jsoup
> * cn.wanghaomiao:JsoupXpath
> * com.jayway.jsonpath:json-path
> * com.github.gedoor:rhino-android
> * com.squareup.okhttp3:okhttp
> * com.github.bumptech.glide:glide
> * org.nanohttpd:nanohttpd
> * org.nanohttpd:nanohttpd-websocket
> * cn.bingoogolapple:bga-qrcode-zxing
> * com.jaredrummler:colorpicker
> * org.apache.commons:commons-text
> * io.noties.markwon:core
> * io.noties.markwon:image-glide
> * com.hankcs:hanlp
> * com.positiondev.epublib:epublib-core
<a href="#readme">
    <img src="https://img.shields.io/badge/-V%E1%BB%81-%C4%91%E1%BA%A7u-orange.svg" alt="#" align="right">
</a>

# Interface-Giao diện [![](https://img.shields.io/badge/-Interface-F5F5F5.svg)](#Interface-Giao-di%E1%BB%87n-)
<img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B1.jpg" width="270"><img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B2.jpg" width="270"><img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B3.jpg" width="270">
<img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B4.jpg" width="270"><img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B5.jpg" width="270"><img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B6.jpg" width="270">

<a href="#readme">
    <img src="https://img.shields.io/badge/-V%E1%BB%81-%C4%91%E1%BA%A7u-orange.svg" alt="#" align="right">
</a>

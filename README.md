# RTSP_LibstreamingSample

下載Libstreaming庫
構建 RTSP 庫涉及對實時流協議的深入理解和對多個 java 媒體 API 的良好掌握，這對每個初學者來說都不容易。幸運的是， 
Fyhertz提供了一個名為libstreaming for android的優秀 RTSP 庫，讓我們的生活變得更輕鬆。
使用這個庫，只需幾行代碼就可以從 android mobile 流式傳輸視頻/音頻。
  
  https://github.com/fyhertz/libstreaming

libstreaming是一個 API，只需幾行代碼，您就可以使用 RTP over UDP 流式傳輸 Android 設備的攝像頭和/或麥克風。

需要 Android 4.0 或更高版本。
支持的編碼器包括 H.264、H.263、AAC 和 AMR。
啟動與某個對等方的流式會話所需的第一步稱為“信號”。在此步驟中，您將聯繫接收方並發送傳入流的描述。您可以通過三種方式使用 libstreaming 來做到這一點。

使用 RTSP 客戶端：如果您想流式傳輸到 Wowza 媒體服務器，這是您的最佳選擇。示例 3說明了該用例。
使用 RTSP 服務器：在這種情況下，手機將充當 RTSP 服務器並等待 RTSP 客戶端請求流。此用例在示例 1中進行了說明。
或者您使用 libstreaming 而不使用 RTSP 協議，並通過您喜歡的協議使用 SDP 向會話發送信號。示例 2說明了該用例。
API 的完整 javadoc 文檔可在此處獲得：

  http: //guigui.us/libstreaming/doc



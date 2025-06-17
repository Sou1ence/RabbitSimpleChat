module com.apokalist.telegram_mini {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.rabbitmq.client;


    opens com.apokalist.telegram_mini to javafx.fxml;
    exports com.apokalist.telegram_mini;
}
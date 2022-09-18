package test.android;

//ENUM contendo as constantes de envio/recebimento de mensagems via bluetooth
public enum MessageConstants {
    MESSAGE_READ(0),
    MESSAGE_WRITE(1),
    MESSAGE_TOAST(2);

    public int val;
    MessageConstants(int i){
        val = i;
    }

    public int getVal() {
        return val;
    }
}

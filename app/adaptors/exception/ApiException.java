package adaptors.exception;

import org.apache.commons.lang3.StringUtils;

public class ApiException extends Exception {

    private int status;
    private String response;

    public ApiException(int status){
        super(toString(status, null));
        this.status = status;
    }

    public ApiException(int status, String response){
        super(toString(status, response));
        this.status = status;
        this.response = response;
    }

    private static String toString(int status, String response){
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toString(status));
        if (StringUtils.isNotEmpty(response)){
            sb.append(": ");
            sb.append(response);
        }
        return sb.toString();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}

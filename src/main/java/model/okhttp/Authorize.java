package model.okhttp;

import java.io.IOException;

public interface Authorize {
    String auth(String username,String password) throws IOException;
}
package com.ivandejesus.prueba_java.domain.ports.in;

import java.io.IOException;
import java.io.InputStream;

public interface DuplicateService {

    byte[] findMatchs(InputStream in, String sheetName) throws IOException;
}

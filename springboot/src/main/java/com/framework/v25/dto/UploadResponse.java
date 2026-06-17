package com.framework.v25.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    private boolean success;
    private String  message;
    private String  filePath;
}

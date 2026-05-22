package com.mypropertyfact.estate.dtos;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class MpfBackupHistoryPageResponse {
    List<MpfBackupHistoryEntryDto> content;
    long totalElements;
    int totalPages;
    int number;
    int size;
}

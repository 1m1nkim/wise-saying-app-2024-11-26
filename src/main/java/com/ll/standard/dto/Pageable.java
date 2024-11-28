package com.ll.standard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class Pageable<T> {
    private final List<T> content;
    private final int totalItems;
    private final int itemsPerPage;
    private final int page;
}

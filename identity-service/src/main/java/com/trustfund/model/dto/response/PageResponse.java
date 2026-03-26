package com.trustfund.model.dto.response;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PageResponse<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PageResponse<T> from(Page<T> page) {
        PageResponse<T> res = new PageResponse<>();
        res.setContent(page.getContent());
        res.setPageNumber(page.getNumber() + 1);
        res.setPageSize(page.getSize());
        res.setTotalElements(page.getTotalElements());
        res.setTotalPages(page.getTotalPages());
        res.setFirst(page.isFirst());
        res.setLast(page.isLast());
        return res;
    }
}

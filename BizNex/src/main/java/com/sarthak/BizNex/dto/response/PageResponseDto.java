package com.sarthak.BizNex.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Generic paged response envelope containing a slice of data and paging metadata")
public class PageResponseDto<T> {
    @Schema(description = "Current page content list")
    private List<T> content;
    @Schema(description = "Zero-based page index returned")
    private int page;
    @Schema(description = "Requested page size (may differ if adjusted by server policies)")
    private int size;
    @Schema(description = "Total number of elements across all pages")
    private long totalElements;
    @Schema(description = "Total number of available pages")
    private int totalPages;
    @Schema(description = "Indicator if this page is the last one")
    private boolean last;

    public static <T> PageResponseDto<T> from(Page<T> page){
        return PageResponseDto.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}

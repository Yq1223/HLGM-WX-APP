package com.wool.controller;

import com.wool.common.Constants;
import com.wool.common.R;
import com.wool.task.ProductFetchTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 商品拉取管理接口（仅管理员可用）
 */
@RestController
@RequestMapping("/api/admin/product-fetch")
public class ProductFetchController {

    private static final Logger log = LoggerFactory.getLogger(ProductFetchController.class);

    private final ProductFetchTask productFetchTask;

    public ProductFetchController(ProductFetchTask productFetchTask) {
        this.productFetchTask = productFetchTask;
    }

    /**
     * 手动触发拼多多商品拉取
     * POST /api/admin/product-fetch/pdd
     */
    @PostMapping("/pdd")
    public R<ProductFetchTask.ManualFetchResult> fetchPdd(HttpServletRequest request) {
        checkAdmin(request);
        log.info("管理员手动触发PDD商品拉取");
        ProductFetchTask.ManualFetchResult result = productFetchTask.manualFetchPdd();
        return R.ok(result);
    }

    /**
     * 手动触发京东商品拉取
     * POST /api/admin/product-fetch/jd
     */
    @PostMapping("/jd")
    public R<ProductFetchTask.ManualFetchResult> fetchJd(HttpServletRequest request) {
        checkAdmin(request);
        log.info("管理员手动触发JD商品拉取");
        ProductFetchTask.ManualFetchResult result = productFetchTask.manualFetchJd();
        return R.ok(result);
    }

    /**
     * 手动触发全部商品拉取
     * POST /api/admin/product-fetch/all
     */
    @PostMapping("/all")
    public R<ProductFetchTask.ManualFetchResult> fetchAll(HttpServletRequest request) {
        checkAdmin(request);
        log.info("管理员手动触发全部商品拉取");
        ProductFetchTask.ManualFetchResult result = productFetchTask.manualFetchAll();
        return R.ok(result);
    }

    /**
     * 检查管理员权限
     */
    private void checkAdmin(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute(Constants.ATTR_USER_ROLE);
        if (role == null || role != Constants.ROLE_ADMIN) {
            throw new com.wool.common.BizException(403, "仅管理员可操作");
        }
    }
}

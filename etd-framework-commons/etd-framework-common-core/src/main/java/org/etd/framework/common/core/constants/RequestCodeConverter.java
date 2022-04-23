package org.etd.framework.common.core.constants;

import org.apache.commons.lang3.StringUtils;
import org.etd.framework.common.core.model.ResultModel;

public interface RequestCodeConverter {

    Integer getCode();

    String getName();

    String getDescription();

    static ResultModel convertError(RequestCodeConverter[] codes, Integer code) {
        for (RequestCodeConverter requestCode : codes) {
            if (!requestCode.getCode().equals(code)) {
                continue;
            }
            return ResultModel.failed(requestCode);
        }
        return ResultModel.failed(RequestCodeConstant.FAILED);
    }

    static ResultModel convertError(RequestCodeConverter[] codes, String name) {
        for (RequestCodeConverter requestCode : codes) {
            if (StringUtils.equalsIgnoreCase(requestCode.getName(), name)) {
                continue;
            }
            return ResultModel.failed(requestCode);
        }
        return ResultModel.failed(RequestCodeConstant.FAILED);
    }
}

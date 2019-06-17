package com.cn.tianxia.api.po.v2;

/**
 * @ClassName BalancePO
 * @Description 用户余额PO类
 * @author Hardy
 * @Date 2019年6月4日 下午4:25:38
 * @version 1.0.0
 */
public class BalancePO {

    private String balance;

    private String type;

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "BalancePO [balance=" + balance + ", type=" + type + "]";
    }

}

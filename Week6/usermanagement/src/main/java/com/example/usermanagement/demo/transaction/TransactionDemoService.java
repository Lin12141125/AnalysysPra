package com.example.usermanagement.demo.transaction;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile("transaction-demo") // 仅当激活此 profile 时加载
public class TransactionDemoService {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionDemoService self; // 注入自身代理，解决内部调用失效问题

    public TransactionDemoService(JdbcTemplate jdbcTemplate, @Lazy TransactionDemoService self) {
        this.jdbcTemplate = jdbcTemplate;
        this.self = self; // @Lazy 避免循环依赖
    }

    // 初始化测试表
    public void initTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS transaction_demo_record");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS transaction_demo_record (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    scenario_name VARCHAR(100) NOT NULL COMMENT '场景名称，用于区分不同实验',
                    step_name VARCHAR(100) NOT NULL COMMENT '步骤名称，用于记录具体操作',
                    remark VARCHAR(255) COMMENT '备注信息',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    // 清楚指定场景的所有记录-->每个实验开始前清空数据
    public void clearScenario(String scenarioName) {
        jdbcTemplate.update("DELETE FROM transaction_demo_record WHERE scenario_name = ?", scenarioName);
    }

    // 查询指定场景的记录，返回格式化字符串列表-->每个实验结束后查询数据验证结果
    public List<String> findScenarioRecords(String scenarioName) {
        return jdbcTemplate.query(
                "SELECT CONCAT(step_name, ' - ', remark) FROM transaction_demo_record WHERE scenario_name = ? ORDER BY id",
                (rs, rowNum) -> rs.getString(1),
                scenarioName
        );
    }

    /*
     * 场景1：REQUIRED方法调用REQUIRES_NEW方法，REQUIRES_NEW方法抛异常
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void methodA() {
        String scenarioName = "CASE1_REQUIRED_calls_REQUIRES_NEW";
        insertRecord(scenarioName, "methodA", "REQUIRED 插入 A 数据");
        try {
            self.methodB(); // 内部调用需要通过代理对象调用才能生效事务
        } catch (RuntimeException exception) {
            System.out.println("[TransactionDemo] methodA 捕获 methodB 异常：" + exception.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB() {
        String scenarioName = "CASE1_REQUIRED_calls_REQUIRES_NEW";
        insertRecord(scenarioName, "methodB", "REQUIRES_NEW 插入 B 数据后故意抛异常");
        throw new RuntimeException("methodB 故意抛异常");
    }

    /*
     * 场景2：REQUIRES_NEW方法调用REQUIRED方法，REQUIRED方法抛异常
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodARequiresNewCallsMethodBRequired() {
        String scenarioName = "CASE2_REQUIRES_NEW_calls_REQUIRED";
        insertRecord(scenarioName, "methodA", "REQUIRES_NEW 插入 A 数据");
        try {
            self.methodBRequired();
        } catch (RuntimeException exception) {
            System.out.println("[TransactionDemo] methodA(REQUIRES_NEW) 捕获 methodB(REQUIRED) 异常：" + exception.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void methodBRequired() {
        String scenarioName = "CASE2_REQUIRES_NEW_calls_REQUIRED";
        insertRecord(scenarioName, "methodB", "REQUIRED 加入 A 的事务，插入 B 数据后故意抛异常");
        throw new RuntimeException("methodBRequired 故意抛异常");
    }

    /*
     * 场景3：REQUIRED方法调用NESTED方法，NESTED方法抛异常
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void methodARequiredCallsMethodBNested() {
        String scenarioName = "CASE3_REQUIRED_calls_NESTED";
        insertRecord(scenarioName, "methodA", "REQUIRED 插入 A 数据");
        try {
            self.methodBNested();
        } catch (RuntimeException exception) {
            System.out.println("[TransactionDemo] methodA(REQUIRED) 捕获 methodB(NESTED) 异常：" + exception.getMessage());
        }
    }

    @Transactional(propagation = Propagation.NESTED)
    public void methodBNested() {
        String scenarioName = "CASE3_REQUIRED_calls_NESTED";
        insertRecord(scenarioName, "methodB", "NESTED 插入 B 数据后故意抛异常，回滚到保存点");
        throw new RuntimeException("methodBNested 故意抛异常");
    }

    // 插入记录到数据库，并打印操作日志
    private void insertRecord(String scenarioName, String stepName, String remark) {
        jdbcTemplate.update(
                "INSERT INTO transaction_demo_record(scenario_name, step_name, remark) VALUES (?, ?, ?)",
                scenarioName,
                stepName,
                remark
        );
        System.out.println("[TransactionDemo] INSERT " + scenarioName + " -> " + stepName + "：" + remark);
    }
}

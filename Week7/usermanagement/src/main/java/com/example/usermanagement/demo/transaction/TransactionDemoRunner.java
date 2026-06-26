package com.example.usermanagement.demo.transaction;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("transaction-demo") // 仅当激活此 profile 时加载
public class TransactionDemoRunner implements CommandLineRunner {

    private final TransactionDemoService transactionDemoService;
    private final ApplicationContext applicationContext;

    public TransactionDemoRunner(TransactionDemoService transactionDemoService, ApplicationContext applicationContext) {
        this.transactionDemoService = transactionDemoService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) {
        // 初始化测试表
        transactionDemoService.initTable();
        // 依次进行三个实验场景
        runCase("CASE1_REQUIRED_calls_REQUIRES_NEW", () -> transactionDemoService.methodA());
        runCase("CASE2_REQUIRES_NEW_calls_REQUIRED", () -> transactionDemoService.methodARequiresNewCallsMethodBRequired());
        runCase("CASE3_REQUIRED_calls_NESTED", () -> transactionDemoService.methodARequiredCallsMethodBNested());
        // 实验执行结束，退出应用
        System.out.println("[TransactionDemo] 实验执行结束，应用即将退出。");
        int exitCode = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(exitCode);
    }

    // 运行单个实验场景-->每个场景前清空数据，场景后查询结果并打印
    private void runCase(String scenarioName, Runnable action) {
        System.out.println("\n==============================");
        System.out.println("[TransactionDemo] 开始场景：" + scenarioName);
        // 清除该场景之前可能残留的数据
        transactionDemoService.clearScenario(scenarioName);
        try {
            action.run();
        } catch (RuntimeException exception) {
            System.out.println("[TransactionDemo] 外层捕获异常：" + exception.getMessage());
        }
        printResult(scenarioName);
    }

    // 打印实验结果-->查询数据库保留的记录数和内容，验证事务传播行为
    private void printResult(String scenarioName) {
        List<String> records = transactionDemoService.findScenarioRecords(scenarioName);
        System.out.println("[TransactionDemo] 数据库最终保留记录数：" + records.size());
        if (records.isEmpty()) {
            System.out.println("[TransactionDemo] 数据库最终没有保留该场景记录");
            return;
        }
        records.forEach(record -> System.out.println("[TransactionDemo] 保留记录：" + record));
    }
}

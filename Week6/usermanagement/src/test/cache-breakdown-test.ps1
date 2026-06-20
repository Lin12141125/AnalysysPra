# 功能：模拟多个线程同时请求同一个热点用户（默认ID=1）
# 用于验证互斥锁是否生效（预期：只有1个线程查数据库，其余命中缓存）

param(
    [string]$BaseUrl = "http://localhost:8080", # API服务器地址
    [string]$Token, # 认证Token，必填
    [int]$Concurrent = 30, # 并发线程数
    [int]$UserId = 1 # 热点用户ID
)

# 检查token是否提供
if ([string]::IsNullOrWhiteSpace($Token)) {
    Write-Host "请传入 Token，例如：-Token xxxxx"
    exit 1
}

# 构造请求头
$headers = @{ Authorization = "Bearer $Token" }
$jobs = @()

# 启动并发任务
1..$Concurrent | ForEach-Object {
    $jobs += Start-Job -ScriptBlock {
        param($u, $h, $id)
        try {
            # 发送GET请求
            $res = Invoke-RestMethod -Uri "$u/api/users/$id" -Method Get -Headers $h
            if ($res.status -eq 200) {
                "OK"
            } else {
                "FAIL status=$($res.status)"
            }
        } catch {
            "ERROR $($_.Exception.Message)"
        }
    } -ArgumentList $BaseUrl, $headers, $UserId
}

# 等待所有任务完成并收集结果
$result = Receive-Job -Job $jobs -Wait -AutoRemoveJob

# 将结果写入文件
$outDir = "target"
if (!(Test-Path $outDir)) {
    New-Item -Path $outDir -ItemType Directory | Out-Null
}

$outFile = Join-Path $outDir "cache-breakdown-output.txt"
$result | Out-File -FilePath $outFile -Encoding utf8

# 统计结果
$okCount = ($result | Where-Object { $_ -eq "OK" }).Count
$errorCount = ($result | Where-Object { $_ -like "ERROR*" }).Count
$failCount = ($result | Where-Object { $_ -like "FAIL*" }).Count

Write-Host "并发请求完成 -> OK=$okCount, FAIL=$failCount, ERROR=$errorCount"
Write-Host "结果文件: $outFile"

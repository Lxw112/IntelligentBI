package com.lxw.springbootinit.bizmq;

import com.lxw.springbootinit.common.ErrorCode;
import com.lxw.springbootinit.constant.CommonConstant;
import com.lxw.springbootinit.constant.RedisConstants;
import com.lxw.springbootinit.controller.ChartController;
import com.lxw.springbootinit.exception.BusinessException;
import com.lxw.springbootinit.manager.AiManager;
import com.lxw.springbootinit.model.entity.Chart;
import com.lxw.springbootinit.service.ChartService;
import com.lxw.springbootinit.utils.ExcelUtils;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class BiMessageConsumer {
    @Resource
    private ChartService chartService;
    @Resource
    private AiManager aiManager;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    //指定程序监听的消息队列和确认机制
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME},ackMode = "MANUAL")

    public void receiveMessage(String message,Channel channel,@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        log.info("receive message: {}",message);
        if (StringUtils.isBlank(message)){
            //消息拒绝
            try {
                channel.basicNack(deliveryTag,false,false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }
        //先修改图表任务状态为”执行中“，等执行成功后，修改为”已完成“、保存图表结果；
        // 执行失败后，状态修改为”失败“，记录任务失败信息
        Chart updateChart = new Chart();
        Chart chart = chartService.getById(message);
        if (chart == null){
            try {
                channel.basicNack(deliveryTag,false,false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"图表为空");
        }
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            try {
                channel.basicNack(deliveryTag,false,false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            handleChartUpdateError(chart.getId(),"更新图表执行中状态失败");
            return;
        }
        //调用Ai
        String result = aiManager.sendMsgToXingHuo(true, buildUserInput(chart));
        System.out.println("----Ai返回的结果是---"+result);
        String[] splits = result.split("'【【【【【'");
        if (splits.length < 3){
            try {
                channel.basicNack(deliveryTag,false,false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            handleChartUpdateError(chart.getId(),"AI生成错误");
            return;
        }
        String genChart = splits[1].trim();
        System.out.println("----图表信息----");
        System.out.println(genChart);
        String genResult = splits[2].trim();
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenResult(genResult);
        updateChartResult.setGenChart(genChart);
        updateChartResult.setStatus(CommonConstant.UPDATE_CHART_SUCCESS);
        boolean updateResult = chartService.updateById(updateChartResult);
        //删除缓存
        deleteUserCache(ChartController.userId);
        if (!updateResult) {
            try {
                channel.basicNack(deliveryTag,false,false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            handleChartUpdateError(chart.getId(),"更新图表成功状态失败");
            return;
        }
        //消息确认
        try {
            channel.basicAck(deliveryTag,false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RabbitListener(queues = {BiMqConstant.BI_DEAD_QUEUE_NAME},ackMode = "MANUAL")
    public void receiveDeadMessage(String message, Channel channel,@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        try {
            Chart chart = chartService.getById(message);
            if (!chart.getStatus().equals("failed")){
                //处理死信消息
                handleChartUpdateError(Long.parseLong(message),"更新图表失败");
                channel.basicAck(deliveryTag,false);
                log.info("死信消息:{} 处理完成",message);
            }else {
                channel.basicAck(deliveryTag,false);
            }
        } catch (Exception e) {
            log.error("死信消息处理失败: {}",message);
            //拒绝消息
            try {
                channel.basicNack(deliveryTag,false,false);
            } catch (Exception ex) {
               log.error("死信消息: {} 拒绝失败",message,ex);
            }
        }
    }
    /**
     * 构建用户输入
     * @param chart
     * @return
     */
    private String buildUserInput(Chart chart){
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();
        //构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        //拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)){
            userGoal += ", 请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        //压缩后的数据
        userInput.append(csvData).append("\n");
        return userInput.toString();
    }
    private void handleChartUpdateError(long chartId,String execMessage){
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage(execMessage);
        boolean b = chartService.updateById(updateChartResult);
        //删除缓存
        new ChartController().deleteUserCache(ChartController.userId);
        if (!b) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }
    public void deleteUserCache(String userId) {
        String keyPattern = RedisConstants.CACHE_CHARTS + userId + "*";
        // 使用 scan 获取匹配的 keys
        Set<String> keys = stringRedisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> matchingKeys = new HashSet<>();
            Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions().match(keyPattern).count(1000).build()
            );
            while (cursor.hasNext()) {
                matchingKeys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
            return matchingKeys;
        });

        // 删除所有匹配的 keys
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
}

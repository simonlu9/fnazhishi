package com.vipflonline.course.service.douyin;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.vipflonline.course.request.douyin.DouyinCourseAddRequest;
import com.vipflonline.course.request.douyin.DouyinCourseMediaAddRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Service
@Slf4j
public class DouyinCourseService {
    private String accessToken;
    private final static String COURSE_REQ_STR= "{\n" +
            "    \"access_token\": \"0801121846336e6d62732b4735557167547171566a62463964413d3d\",\n" +
            "    \"product_type\": 1,\n" +
            "    \"product\": {\n" +
            "        \"common_product_params\": {\n" +
            "            \"appid\": \"tt8d0ffbb2123003fd01\",\n" +
            "            \"first_class\": 30000,\n" +
            "            \"second_class\": 30100,\n" +
            "            \"title\": \"实况9000句之  旅游口语-10天速成\",\n" +
            "            \"purchase_precaution\": \"小程序内购买课程为课程兑换权益，购买成功后将自动兑换到您的账户内，可在外文在线小程序中重复学习观看\",\n" +
            "            \"product_fulfillment_lst\": [\n" +
            "                {\n" +
            "                    \"fulfillment_content\": {\n" +
            "                        \"fulfillment_uri\": \"product/resource/e3b296a39aba14d23dfa89b879224b6f\",\n" +
            "                        \"text\": \"计划行程-询问路线\",\n" +
            "                        \"name\": \"第1课时\"\n" +
            "                    },\n" +
            "                    \"fulfillment_type\": 1\n" +
            "                },\n" +
            "                 {\n" +
            "                    \"fulfillment_content\": {\n" +
            "                        \"fulfillment_uri\": \"product/resource/cba1a1aecc1b1213825cd15396bccd61\",\n" +
            "                        \"text\": \"计划行程-制定旅游计划\",\n" +
            "                        \"name\": \"第2课时\"\n" +
            "                    },\n" +
            "                    \"fulfillment_type\": 1\n" +
            "                }\n" +
            "            ],\n" +
            "            \"industry_type\": 1,\n" +
            "            \"price_info\": {\n" +
            "                \"unit\": \"节\",\n" +
            "                \"price\": 19900\n" +
            "            },\n" +
            "            \"path_info_lst\": [\n" +
            "                {\n" +
            "                    \"path\": \"pages/study/course-details/course-details\",\n" +
            "                    \"query\": {\n" +
            "                        \"courseId\":\"ff808081877864ea018779d88d9d1d48\",\n" +
            "                        \"course_id\": \"ff808081877864ea018779d88d9d1d48\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ],\n" +
            "            \"product_detail_lst\": [\n" +
            "                {\n" +
            "                      \"text\": \"课程详情介绍\",\n" +
            "                    \"img_uri\": \"tos-cn-i-b2i6zad4el/1b5d1c9dca64ad2f2e3d1e497f064d62\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"anchor_info\": {\n" +
            "                \"video_anchor_info\": {\n" +
            "                    \"anchor_title\": \"第一节\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"product_img_uri\": \"tos-cn-i-b2i6zad4el/1b5d1c9dca64ad2f2e3d1e497f064d62\"\n" +
            "        },\n" +
            "        \"course_params\": {\n" +
            "            \"institution_id\": \"E_7223285027843375108\",\n" +
            "            \"course_num\": 32,\n" +
            "            \"refund_label\": {\n" +
            "                \"type\": 1,\n" +
            "                \"day_before_use_info\": {\n" +
            "                    \"day\": 7\n" +
            "                }\n" +
            "            },\n" +
            "            \"use_label\": {\n" +
            "                \"valid_date\": \"2099-12-01\"\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";
    private RestTemplate restTemplate = new RestTemplate();

    @Resource(name = "RedisTemplate")
    private RedisTemplate redisTemplate;

    private JSONObject executePostRequest(String url, JSONObject params){
        log.info("请求信息:{}",params);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(params.toJSONString(), headers);
        ResponseEntity<JSONObject> response;
        response = restTemplate.postForEntity(url, entity, JSONObject.class);
        JSONObject result = response.getBody();
        log.info("{}获取结果{}" ,url, result.toJSONString());
        if(result.getIntValue("err_no")==10003){
            String uri =  result.getString("err_msg").split(":")[1];
            JSONObject errObject = new JSONObject();
            errObject.put("resource_uri",uri);
            return errObject;
        }else if(result.getIntValue("err_no")==10141||result.getIntValue("err_no")==10140){
            String productId =  result.getString("err_msg").replaceAll("[^\\d]+","");
            JSONObject errObject = new JSONObject();
            errObject.put("product_id",productId);
            return errObject;
        }else if(result.getIntValue("err_no")>0){
            if(result.getIntValue("err_no")==10001){
                accessToken = null;
            }
            throw new RuntimeException(result.toJSONString());
        }
        return result.getJSONObject("data");
    }

    private String getAccessToken(){
        if(accessToken!=null){
            return accessToken;
        }
        JSONObject jsonObject = JSONObject.parseObject("{\n" +
                "  \"appid\": \"appid\",\n" +
                "  \"secret\": \"secret\",\n" +
                "  \"grant_type\": \"client_credential\"\n" +
                "}");

        JSONObject result = executePostRequest("https://developer.toutiao.com/api/apps/v2/token",jsonObject);
        accessToken = result.getString("access_token");
        return accessToken;
    }

    private JSONObject checkResource(String resourceUri){
        String token = getAccessToken();
        JSONObject jsonObject = JSONObject.parseObject("{\n" +
                "  \"access_token\": \"0801121846735561486f5a48744d744157456a554a68446d52773d3d\",\n" +
                "  \"appid\": \"appid\",\n" +
                "  \"resource_uri\": \"product/resource/abcd\"\n" +
                "}");
        jsonObject.put("access_token",token);
        jsonObject.put("resource_uri",resourceUri);
        JSONObject result = executePostRequest("https://developer-product.zijieapi.com/product/api/query_resource_status",jsonObject);
        return result;
    }

    private JSONObject uploadResource(DouyinCourseMediaAddRequest request){
        String path = URLUtil.toURI(request.getUri()).getPath();
        String resourceUri = (String)redisTemplate.opsForValue().get(path);
        if(resourceUri==null){
            String token = getAccessToken();
            JSONObject jsonObject = JSONObject.parseObject("{\n" +
                    "  \"access_token\": \"0801121846336c634975494f592f5973475a4242493756754d513d3d\",\n" +
                    "  \"appid\": \"appid\",\n" +
                    "  \"resource_type\": 2,\n" +
                    "  \"resource_url\": \"http://vod.vipflonline.com/922011a4vodcq1300677316/78e4bf77243791581366744219/4b8b43v8pa0A.mp4?t=64d36694&us=72d4cd1101&sign=1543b81d566ad6cb965142c17e0ee44f\"" +
                    "}");
            jsonObject.put("access_token",token);
            jsonObject.put("resource_type",request.getType());
            jsonObject.put("resource_url",request.getUri());

            JSONObject result =  executePostRequest("https://developer-product.zijieapi.com/product/api/upload_resource",jsonObject);;

            redisTemplate.opsForValue().set(path,result.getString("resource_uri"));
            return result;
        }else{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("resource_uri",resourceUri);
            return jsonObject;
        }


    }

    public String uploadCourse(DouyinCourseAddRequest request){
        String token = getAccessToken();
        JSONObject jsonObject = JSONObject.parseObject(COURSE_REQ_STR);
        jsonObject.put("access_token",token);
        JSONObject productParam = jsonObject.getJSONObject("product").getJSONObject("common_product_params");
        productParam.put("title",request.getName());
        productParam.getJSONObject("price_info").put("price",(request.getPrice().multiply(BigDecimal.valueOf(100))).toBigInteger());
        //设置课程内容
        List<String> resourceUrls = new ArrayList<>();
        String mediaStr = " {\n" +
                "                    \"fulfillment_content\": {\n" +
                "                        \"fulfillment_uri\": \"product/resource/cba1a1aecc1b1213825cd15396bccd61\",\n" +
                "                        \"text\": \"计划行程-制定旅游计划\",\n" +
                "                        \"name\": \"第2课时\"\n" +
                "                    },\n" +
                "                    \"fulfillment_type\": 1\n" +
                "                }";
        JSONArray fulfillmemtList = productParam.getJSONArray("product_fulfillment_lst");
        fulfillmemtList.clear();
        request.getPeriods().forEach(period->{
           // JSONObject result = uploadResource(period);
            String resourceUri =  uploadResource(period).getString("resource_uri");
            resourceUrls.add(resourceUri);
            JSONObject element =  JSONObject.parseObject(mediaStr);
            JSONObject contentObject = element.getJSONObject("fulfillment_content");
            contentObject.put("fulfillment_uri",resourceUri);
            contentObject.put("text",period.getText());
            contentObject.put("name",period.getName());
            fulfillmemtList.add(element);

        });



        //设置路径信息
        productParam.getJSONArray("path_info_lst").getJSONObject(0).getJSONObject("query").put("courseId",request.getCourseId());
        productParam.getJSONArray("path_info_lst").getJSONObject(0).getJSONObject("query").put("course_id",request.getCourseId());
        //设置详情信息
        //上传详情图
        DouyinCourseMediaAddRequest detailRequest = new DouyinCourseMediaAddRequest();
        detailRequest.setType(31);
        detailRequest.setUri(request.getDetail());
        JSONObject detailResult = uploadResource(detailRequest);
        JSONObject detailObject = productParam.getJSONArray("product_detail_lst").getJSONObject(0);
        detailObject.put("img_uri",detailResult.get("resource_uri"));
        resourceUrls.add(detailResult.getString("resource_uri"));
        for(String resourceUrl:resourceUrls){
            JSONObject uploadStatus = checkResource(resourceUrl);
            if(uploadStatus.getIntValue("status")!=2){
                return resourceUrl+"资源上传中";
            }
        }

        productParam.put("product_img_uri",detailResult.getString("resource_uri"));

        //检查资源状态



        JSONObject uploadResult = executePostRequest("https://developer-product.zijieapi.com/product/api/add",jsonObject);

        return uploadResult.getString("product_id");

    }

   

   
}

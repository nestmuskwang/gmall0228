package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;
    @Autowired
    private  CartCookieHander cartCookieHandler;

    @Reference
    private ManageService manageService;

    @RequestMapping(value = "addToCart",method = RequestMethod.POST)
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response, Model model){
        //先判读cartInfo 中是否有该商品，有则数据加1，放到缓存，没有新建，skuid,userId [ssoz中取得]
        //取得userId,skuId,skunum
        String skuNum = request.getParameter("skuNum");
        String skuId = request.getParameter("skuId");
        String userId = (String) request.getAttribute("userId");

        //判读用户是否登录
        if (userId!=null){
            cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        }else{
            // 说明用户没有登录没有登录放到cookie中

            cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));
        }
        //获取skuinfo 信息,后台存储什么是根据前台需要来确定的

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);


        //保存商品对象
        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response, Model model) {
        //判断是否登录
        // 判断用户是否登录，登录了从redis中，redis中没有，从数据库中取
        // 没有登录，从cookie中取得

        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartList =null;
        List<CartInfo> cartListFromCookie = cartCookieHandler.getCartList(request);
        if (userId != null) {
            if (cartListFromCookie!=null && cartListFromCookie.size()>0){
                //合并购物车，cookie --》db
                cartList= cartService.mergeToCartList(cartListFromCookie,userId);
                //从cookie中删除
                cartCookieHandler.deleteCartCookie(request,response);
            }else {
                // 从redis中取得，或者从数据库中
                cartList = cartService.getCartList(userId);
            }


            request.setAttribute("cartList",cartList);
        }else{
           cartList = cartCookieHandler.getCartList(request);
            request.setAttribute("cartList",cartList);
        }
        return "cartList";
    }

    //参数，看内部需要使用那些方法，cookie,cartService
    @RequestMapping(value = "checkCart",method = RequestMethod.POST)
    @LoginRequire(autoRedirect = false)
    @ResponseBody
    public  void  checkCart(HttpServletRequest request, HttpServletResponse response,CartInfo cartInfo){
        //取得user ID判断是否登录，
        String  userId = (String) request.getAttribute("userId");
        String isChecked = request.getParameter("skuId");
        String skuId =request.getParameter("skuId");
        if (userId != null) {
            //登录 ,将数据取出来操作redis=== 封装成一个方法操作数据库的
            cartService.checkCart(userId,skuId,isChecked);
        }else {
            //未登录操作cookie
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }
    }

    @RequestMapping("toTrade")
    @LoginRequire(autoRedirect = true)
    public  String  toTrade(HttpServletRequest request,HttpServletResponse response){
        //取得UserId
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cookieHandlerCartList = cartCookieHandler.getCartList(request);
        //循环遍历cookie中的值，跟db进行合并
        if (cookieHandlerCartList!=null && cookieHandlerCartList.size()>0){
            List<CartInfo> cartInfoList = cartService.mergeToCartList(cookieHandlerCartList, userId);
            cartCookieHandler.deleteCartCookie(request,response);
        }
        return "redirect://order.gmall.com/trade";

    }


    }

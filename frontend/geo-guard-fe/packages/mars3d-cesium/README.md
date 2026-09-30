# CesiumJS

[![Build Status](https://github.com/CesiumGS/cesium/actions/workflows/dev.yml/badge.svg)](https://github.com/CesiumGS/cesium/actions/workflows/dev.yml)
[![npm](https://img.shields.io/npm/v/cesium)](https://www.npmjs.com/package/cesium)
[![Docs](https://img.shields.io/badge/docs-online-orange.svg)](https://cesium.com/learn/)

![Cesium](https://github.com/CesiumGS/cesium/wiki/logos/Cesium_Logo_Color.jpg)

CesiumJS is a JavaScript library for creating 3D globes and 2D maps in a web browser without a plugin. It uses WebGL for hardware-accelerated graphics, and is cross-platform, cross-browser, and tuned for dynamic-data visualization.

Built on open formats, CesiumJS is designed for robust interoperability and scaling for massive datasets.

---

[**Examples**](https://sandcastle.cesium.com/) :earth_asia: [**Docs**](https://cesium.com/learn/cesiumjs-learn/) :earth_americas: [**Website**](https://cesium.com/cesiumjs) :earth_africa: [**Forum**](https://community.cesium.com/) :earth_asia: [**User Stories**](https://cesium.com/user-stories/)

---

# mars3d-cesium

Mars3D修改版Cesium库， 因为cesium本身也有一些bug或需要优化的地方，Mars3D使用的cesium也修改了部分cesium源码文件。我们修改Cesium的原则：

- 主要扩展都在mars3d类库中，没法外部扩展的，才会在内部去修改cesium底层；
- 尽量不改变ceisum本身逻辑，通过参数来可控改变；
- 并且所有修改都有完整注释说明。

> 更多说明：http://mars3d.cn/dev/guide/start/cesium.html

<p align="center">
<img src="//mars3d.cn/logo.png" width="300px" />
</p>

<p align="center">🌎 Mars3D 一款基于WebGL技术实现的三维地球客户端开发平台</p>

<p align="center">
  <a target="_black" href="https://www.npmjs.com/package/mars3d">
    <img alt="Npm version" src="https://img.shields.io/npm/v/mars3d.svg?style=flat&logo=npm&label=版本号" />
  </a>
  <a target="_black" href="https://www.npmjs.com/package/mars3d">
    <img alt="Npm downloads" src="https://img.shields.io/npm/dt/mars3d?style=flat&logo=npm&label=下载量" />
  </a>
  <a target="_black" href="https://github.com/marsgis/mars3d">
    <img alt="GitHub stars" src="https://img.shields.io/github/stars/marsgis/mars3d?style=flat&logo=github" />
  </a>
  <a target="_black" href="https://gitee.com/marsgis/mars3d">
    <img src="https://gitee.com/marsgis/mars3d/badge/star.svg?theme=dark" alt="star" />
  </a>
</p>

[**English**](./README_EN.md) |[**中文**](./README.md)

当前仓库包含SDK更新日志、和其他仓库导航。

Github：[https://github.com/marsgis/mars3d](https://github.com/marsgis/mars3d)

国内Gitee：[https://gitee.com/marsgis/mars3d](https://gitee.com/marsgis/mars3d)

更新日志: [CHANGE.md](./CHANGE.md)

## 功能示例

对平台每个功能编写的案例代码，可以阅读每个示例代码来学习各功能，与官网功能示例一致效果。

| 说明     | 教程                                                          | 视频                                                 | Github源码                                                              | 国内Gitee源码                                                          |
| -------- | ------------------------------------------------------------- | ---------------------------------------------------- | ----------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| 原生JS版 | [查看](http://mars3d.cn/dev/guide/project/example-es5.html)   | [查看](https://www.bilibili.com/video/BV1Hg411o7Js/) | [mars3d-es5-example](https://github.com/marsgis/mars3d-es5-example)     | [mars3d-es5-example](https://gitee.com/marsgis/mars3d-es5-example)     |
| Vue版    | [查看](http://mars3d.cn/dev/guide/project/example-vue.html)   | [查看](https://www.bilibili.com/video/BV1PL41177SS/) | [mars3d-vue-example](https://github.com/marsgis/mars3d-vue-example)     | [mars3d-vue-example](https://gitee.com/marsgis/mars3d-vue-example)     |
| React版  | [查看](http://mars3d.cn/dev/guide/project/example-react.html) | [查看](https://www.bilibili.com/video/BV1xY4y1t7NR/) | [mars3d-react-example](https://github.com/marsgis/mars3d-react-example) | [mars3d-react-example](https://gitee.com/marsgis/mars3d-react-example) |

## 最简项目模板

提供了面向不同技术栈的最简项目模板，您可以直接使用或参考这些项目模板，了解SDK类库在不同技术栈项目中如何引入使用，快速搭建开发自己的项目。

| 说明          | 视频                                                 | Github源码                                                                    | 国内Gitee源码                                                                |
| ------------- | ---------------------------------------------------- | ----------------------------------------------------------------------------- | ---------------------------------------------------------------------------- | ---------------------------------------------------------- | --- |
| 原生JS技术栈  | [查看](https://www.bilibili.com/video/BV1za41177ZW/) | [mars3d-es5-template](https://github.com/marsgis/mars3d-es5-template)         | [mars3d-es5-template](https://gitee.com/marsgis/mars3d-es5-template)         |
| Vue技术栈     | [查看](https://www.bilibili.com/video/BV17P4y1F7gv/) | [mars3d-vue-template](https://github.com/marsgis/mars3d-vue-template)         | [mars3d-vue-template](https://gitee.com/marsgis/mars3d-vue-template)         |
| React技术栈   | [查看](https://www.bilibili.com/video/BV1nZ4y1b7W7/) | [mars3d-react-template](https://github.com/marsgis/mars3d-react-template)     | [mars3d-react-template](https://gitee.com/marsgis/mars3d-react-template)     |
| Angular技术栈 | [查看](https://www.bilibili.com/video/BV13Y411K7xs/) | [mars3d-angular-template](https://github.com/marsgis/mars3d-angular-template) | [mars3d-angular-template](https://gitee.com/marsgis/mars3d-angular-template) |
| CS桌面端程序  | [查看](https://www.bilibili.com/video/BV1N94y1R7ds/) | [mars3d-electron](https://github.com/marsgis/mars3d-electron)                 | [mars3d-electron](https://gitee.com/marsgis/mars3d-electron)                 |
| <!--          | APP移动端程序                                        | [查看](https://www.bilibili.com/video/BV18F41157qR/)                          | [mars3d-uni-app](https://github.com/marsgis/mars3d-uni-app)                  | [mars3d-uni-app](https://gitee.com/marsgis/mars3d-uni-app) | --> |

## 基础项目

基础项目是具备一个GIS项目常见功能的项目模板，只用更换数据及叠加业务即可完成项目交付。
如果需要更多功能可以考虑购买[付费项目模板](http://mall.marsgis.cn/#/product-list?category=project)。

| 说明                     | 教程                                                       | 视频                                                 | Github源码                                                                      | 国内Gitee源码                                                                  |
| ------------------------ | ---------------------------------------------------------- | ---------------------------------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| 基础项目 原生JS版        | [查看](http://mars3d.cn/dev/guide/project/jcxm-es5.html)   | [查看](https://www.bilibili.com/video/BV1nF41157Rn/) | [mars3d-es5-project](https://github.com/marsgis/mars3d-es5-project)             | [mars3d-es5-project](https://gitee.com/marsgis/mars3d-es5-project)             |
| 基础项目 Vue版           | [查看](http://mars3d.cn/dev/guide/project/jcxm-vue.html)   | [查看](https://www.bilibili.com/video/BV1JF411q7Ut/) | [mars3d-vue-project](https://github.com/marsgis/mars3d-vue-project)             | [mars3d-vue-project](https://gitee.com/marsgis/mars3d-vue-project)             |
| 基础项目 Vue版+admin管理 | 无                                                         | 无                                                   | [mars3d-vue-project-admin](https://github.com/marsgis/mars3d-vue-project-admin) | [mars3d-vue-project-admin](https://gitee.com/marsgis/mars3d-vue-project-admin) |
| 三维地球APP项目          | [查看](http://mars3d.cn/dev/guide/project/app-vue.html)    | [查看](https://www.bilibili.com/video/BV1iY4y1t7jk/) | [mars3d-vue-app](https://github.com/marsgis/mars3d-vue-app)                     | [mars3d-vue-app](https://gitee.com/marsgis/mars3d-vue-app)                     |
| 基础项目 React版         | [查看](http://mars3d.cn/dev/guide/project/jcxm-react.html) | [查看](https://www.bilibili.com/video/BV1n3411u7Dm/) | [mars3d-react-project](https://github.com/marsgis/mars3d-react-project)         | [mars3d-react-project](https://gitee.com/marsgis/mars3d-react-project)         |

## 其他

| 说明         | Github                                                    | 国内Gitee                                                |
| ------------ | --------------------------------------------------------- | -------------------------------------------------------- |
| 部分开源插件 | [mars3d-plugin](https://github.com/marsgis/mars3d-plugin) | [mars3d-plugin](https://gitee.com/marsgis/mars3d-plugin) |

## 平台介绍

`Mars3D平台` 是[火星科技](http://marsgis.cn/)研发的一款基于 WebGL 技术实现的三维客户端开发平台，基于[Cesium](https://cesium.com/cesiumjs/)优化提升与B/S架构设计，支持多行业扩展的轻量级高效能GIS开发平台，能够免安装、无插件地在浏览器中高效运行，并可快速接入与使用多种GIS数据和三维模型，呈现三维空间的可视化，完成平台在不同行业的灵活应用。

## 学习资料

- 开发教程：[http://mars3d.cn/dev/](http://mars3d.cn/dev/)
- API文档：[http://mars3d.cn/api/](http://mars3d.cn/api/)
- Mars3D官网：[http://mars3d.cn](http://mars3d.cn)

### 反馈问题

为了方便交流及留存，所有bug及建议均统一提交至：[github issues](https://github.com/marsgis/mars3d/issues) 或
[国内gitee issues](https://gitee.com/marsgis/mars3d/issues)

## 平台相关效果

功能示例：
![image](http://marsgis.cn/img/project/mars3d-doc/example1.jpg)

基础项目：[在线体验](http://mars3d.cn/project/vue/jcxm.html)

![image](http://marsgis.cn/img/project/mars3d-vue-project-jcxm/1.jpg)

电子沙盘项目：[在线体验](http://mars3d.cn/project/es5/dzsp.html)

![image](http://marsgis.cn/img/project/mars3d-es5-project-dzsp/2.jpg)

智慧园区项目：[在线体验](http://mars3d.cn/project/vue/zhyq.html)

![image](http://marsgis.cn/img/project/mars3d-vue-project-zhyq/1.jpg)

综合态势项目：[在线体验](http://mars3d.cn/project/vue/login.html)

![image](http://marsgis.cn/img/project/mars3d-vue-project-zhts/2.jpg)

## 版权说明

1. Mars3D平台由[火星科技](http://marsgis.cn/)自主研发，拥有所有权利。
2. 任何个人或组织可以在遵守相关要求下可以免费无限制使用。

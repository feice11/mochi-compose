#!/usr/bin/env python3
# Copyright 2026, mochi-compose contributors
# SPDX-License-Identifier: Apache-2.0
"""
给每个基础名生成中文搜索关键词，写成 `ionicons-zh.txt`（`名字\t关键词 关键词…`）。
先按整名查 PHRASES（合成词：冰淇淋、二维码……），再把名字里每个单词按 WORDS 翻译后拼上。
用法：python zh_keywords.py <ionicons-index.txt> <输出文件>
"""

import sys
from pathlib import Path

WORDS = {
    "accessibility": "无障碍 辅助功能", "american": "美式", "add": "添加 加号 新建", "airplane": "飞机 飞行模式", "alarm": "闹钟",
    "albums": "相册 专辑", "alert": "提醒 警告 感叹号", "analytics": "分析 统计", "aperture": "光圈 镜头",
    "apps": "应用 九宫格", "archive": "归档 存档", "arrow": "箭头", "at": "艾特 邮箱",
    "attach": "附件 回形针", "back": "返回 后退 左", "backspace": "退格 删除", "bag": "购物袋 包",
    "ball": "球", "balloon": "气球", "ban": "禁止 屏蔽", "bandage": "创可贴 绷带", "bar": "柱", "barbell": "杠铃 健身",
    "barcode": "条形码", "baseball": "棒球", "basket": "篮子 购物篮", "basketball": "篮球", "battery": "电池 电量",
    "beaker": "烧杯 实验", "bed": "床 睡眠", "beer": "啤酒", "bicycle": "自行车 骑行", "bin": "垃圾桶",
    "binoculars": "望远镜", "bluetooth": "蓝牙", "boat": "船", "body": "身体 人体", "bonfire": "篝火",
    "book": "书 阅读", "bookmark": "书签 收藏", "bookmarks": "书签 收藏", "bowling": "保龄球", "box": "方框",
    "branch": "分支", "briefcase": "公文包 工作", "browsers": "浏览器 窗口", "brush": "画笔 刷子", "bug": "虫子 调试",
    "build": "构建 扳手 工具", "bulb": "灯泡 想法", "buoy": "浮标", "bus": "公交 巴士", "business": "公司 商业 大楼",
    "button": "按钮", "cafe": "咖啡 咖啡馆", "calculator": "计算器", "calendar": "日历 日程", "call": "电话 拨号",
    "camera": "相机 拍照", "car": "汽车", "card": "卡 银行卡", "caret": "三角箭头", "cart": "购物车",
    "cash": "现金 钱", "cellular": "信号 蜂窝", "charging": "充电", "chart": "图表", "chatbox": "消息 聊天",
    "chatbubble": "气泡 聊天 消息", "chatbubbles": "气泡 聊天 对话", "check": "勾选", "checkbox": "复选框 勾选",
    "checkmark": "对勾 完成", "chevron": "箭头 尖括号", "chip": "芯片", "circle": "圆形",
    "clear": "空白", "clipboard": "剪贴板", "close": "关闭 叉", "closed": "关闭", "cloud": "云",
    "cloudy": "多云 阴天", "code": "代码", "cog": "齿轮 设置", "collapse": "收起 折叠", "color": "颜色",
    "commit": "提交", "compare": "对比", "compass": "指南针 罗盘 发现", "construct": "工具 施工", "contract": "收缩 缩小",
    "contrast": "对比度", "controller": "手柄", "copy": "复制", "cream": "奶油", "create": "编辑 新建 写",
    "crop": "裁剪", "cube": "立方体 3D", "cut": "剪切 剪刀", "dead": "耗尽", "desktop": "桌面 显示器 电脑",
    "diamond": "钻石", "dice": "骰子", "disc": "光盘 唱片", "dislike": "不喜欢", "document": "文档 文件",
    "documents": "文档 文件", "done": "完成", "down": "向下 下", "download": "下载", "duplicate": "复制 副本",
    "ear": "耳朵 听", "earth": "地球 世界", "easel": "画架 演示", "egg": "鸡蛋", "ellipse": "椭圆 圆",
    "ellipses": "省略号", "ellipsis": "更多 省略号", "enter": "进入", "exit": "退出", "expand": "展开 放大 全屏",
    "extension": "扩展 插件", "eye": "眼睛 查看 显示", "eyedrop": "取色 吸管", "fast": "快", "female": "女 女性",
    "file": "文件", "fill": "填充", "film": "电影 胶片", "filter": "筛选 过滤", "finger": "手指",
    "fish": "鱼", "fitness": "健身 运动", "flag": "旗帜 标记 举报", "flame": "火焰 热门", "flash": "闪电 闪光灯",
    "flashlight": "手电筒", "flask": "烧瓶 实验", "flower": "花", "folder": "文件夹", "food": "食物",
    "football": "足球", "footsteps": "脚印 步数", "forward": "前进 右", "four": "四", "full": "满",
    "funnel": "漏斗 筛选", "game": "游戏", "gift": "礼物", "git": "版本控制", "glasses": "眼镜",
    "globe": "地球 网络 语言", "golf": "高尔夫", "grid": "网格", "half": "一半", "hammer": "锤子",
    "hand": "手", "handle": "提手", "happy": "开心 笑脸", "hardware": "硬件", "headset": "耳机 客服",
    "heart": "心 喜欢 收藏", "help": "帮助 问号", "high": "高", "home": "主页 首页 家", "horizontal": "水平 横向",
    "hourglass": "沙漏 等待", "ice": "冰", "id": "身份", "image": "图片 照片", "images": "图片 相册",
    "in": "入", "infinite": "无限 循环", "information": "信息 说明", "invert": "反色", "journal": "日记 笔记",
    "key": "钥匙 密码", "keypad": "拨号盘 键盘", "landscape": "横屏", "language": "语言 翻译",
    "laptop": "笔记本电脑", "layers": "图层", "leaf": "叶子 环保", "left": "左", "library": "图书馆 资料库",
    "link": "链接", "list": "列表", "locate": "定位", "location": "位置 定位", "lock": "锁",
    "log": "登录", "low": "低", "magnet": "磁铁", "mail": "邮件 信封", "male": "男 男性", "man": "男人",
    "map": "地图", "medal": "奖牌", "medical": "医疗", "medium": "中", "medkit": "急救箱 医药",
    "megaphone": "喇叭 公告", "menu": "菜单", "merge": "合并", "mic": "麦克风 录音", "mode": "模式",
    "moon": "月亮 夜间 深色", "move": "移动", "musical": "音乐", "mute": "静音", "navigate": "导航",
    "network": "网络", "newspaper": "新闻 报纸", "night": "夜晚", "note": "音符", "notes": "音符",
    "notifications": "通知 铃铛", "nuclear": "核 辐射", "number": "数字", "nutrition": "营养 水果",
    "off": "关闭", "offline": "离线", "on": "开启", "open": "打开 外部链接", "options": "选项 调节 滑块",
    "out": "出", "palette": "调色板 颜色", "paper": "纸", "partly": "局部", "pause": "暂停", "paw": "爪印 宠物",
    "pencil": "铅笔 编辑", "people": "人们 群组 联系人", "person": "个人 用户 联系人", "phone": "手机",
    "pie": "饼", "pin": "图钉 位置", "pint": "啤酒杯", "pizza": "披萨", "plane": "飞机", "planet": "星球",
    "play": "播放", "podium": "领奖台 排名", "portrait": "竖屏", "power": "电源 开关", "pricetag": "价签 标签",
    "pricetags": "价签 标签", "print": "打印", "prism": "棱镜", "pull": "拉取", "pulse": "心率 脉搏 波形",
    "push": "推送", "puzzle": "拼图", "qr": "二维码", "radio": "电台 收音机", "rainy": "下雨 雨",
    "reader": "阅读 文章", "receipt": "收据 账单", "recording": "录音 录像", "redo": "重做", "refresh": "刷新",
    "reload": "重新加载 刷新", "remove": "移除 减号", "reorder": "排序 拖动", "repeat": "重复 循环",
    "request": "请求", "resize": "调整大小", "restaurant": "餐厅 餐具", "return": "回车 返回", "reverse": "翻转 反转",
    "ribbon": "丝带 徽章", "right": "右", "rocket": "火箭 发布", "rose": "玫瑰", "sad": "难过 哭脸",
    "save": "保存", "scale": "秤 体重", "scan": "扫描 扫码", "school": "学校 毕业", "search": "搜索 放大镜",
    "send": "发送", "server": "服务器 数据库", "settings": "设置 齿轮", "shapes": "形状", "share": "分享",
    "shield": "盾牌 安全 保护", "shirt": "衬衫 衣服", "shuffle": "随机 乱序", "sign": "标志", "skip": "跳过 切歌",
    "skull": "骷髅", "slash": "斜杠", "snow": "雪 下雪", "social": "社交", "sparkles": "闪光 星星 特效 AI",
    "speedometer": "速度表 仪表盘 性能", "sport": "运动", "square": "方形 正方形", "stacked": "堆叠",
    "star": "星星 收藏 评分", "stats": "统计", "stop": "停止", "stopwatch": "秒表 计时",
    "storefront": "商店 店铺", "subway": "地铁", "sunny": "晴天 太阳 浅色 亮度", "swap": "交换 切换",
    "sync": "同步", "tablet": "平板", "telescope": "望远镜", "tennisball": "网球", "terminal": "终端 命令行",
    "text": "文字 文本", "thermometer": "温度计 温度", "three": "三", "thumbs": "点赞 拇指",
    "thunderstorm": "雷雨", "ticket": "票 门票 优惠券", "time": "时间 时钟", "timer": "计时器 倒计时",
    "today": "今天 日历", "toggle": "开关", "trail": "小路", "train": "火车", "transgender": "跨性别",
    "trash": "垃圾桶 删除", "tray": "托盘 收件箱", "trending": "趋势", "triangle": "三角形",
    "trophy": "奖杯 成就", "tv": "电视", "two": "二", "umbrella": "雨伞", "undo": "撤销", "unlink": "断开链接",
    "unread": "未读", "up": "向上 上", "upload": "上传", "vertical": "竖直 纵向", "videocam": "摄像机 视频",
    "volume": "音量 声音", "walk": "步行 走路", "wallet": "钱包 支付", "wand": "魔棒", "warning": "警告 注意",
    "watch": "手表", "water": "水 水滴", "wifi": "无线网络 WiFi", "wine": "红酒 酒杯", "woman": "女人",
    "working": "运行中",
}

PHRASES = {
    "american-football": "橄榄球", "ice-cream": "冰淇淋", "fast-food": "快餐 汉堡", "game-controller": "游戏手柄",
    "finger-print": "指纹 解锁", "help-buoy": "救生圈 帮助", "qr-code": "二维码", "id-card": "身份证 证件",
    "paper-plane": "纸飞机 发送", "trail-sign": "路标 指示牌", "pie-chart": "饼图", "bar-chart": "柱状图",
    "stats-chart": "统计图", "hardware-chip": "芯片 处理器", "extension-puzzle": "拼图 扩展 插件",
    "partly-sunny": "晴转多云", "cloudy-night": "夜间多云", "male-female": "性别 男女", "bowling-ball": "保龄球",
    "file-tray": "收件箱 托盘", "color-wand": "魔棒 自动美化", "color-fill": "油漆桶 填充",
    "color-filter": "滤镜", "eye-off": "隐藏 不可见", "log-in": "登录", "log-out": "退出登录", "lock-open": "解锁",
    "lock-closed": "锁定 上锁", "code-slash": "代码", "git-pull-request": "合并请求", "share-social": "分享 社交",
    "musical-note": "音符 音乐", "musical-notes": "音符 音乐", "camera-reverse": "切换镜头 翻转相机",
    "radio-button-on": "单选 选中", "radio-button-off": "单选 未选", "invert-mode": "反色 深浅模式",
    "car-sport": "跑车", "phone-portrait": "手机 竖屏", "phone-landscape": "手机 横屏", "trash-bin": "垃圾桶 删除",
    "battery-dead": "没电", "battery-full": "满电", "battery-half": "半电量", "battery-charging": "充电中",
    "heart-half": "半颗心 评分", "star-half": "半星 评分", "sad": "难过 哭脸", "happy": "开心 笑脸",
    "cloud-offline": "离线 无网络", "notifications-off": "免打扰 关闭通知", "volume-mute": "静音",
    "volume-off": "静音 关闭声音", "videocam-off": "关闭摄像头", "mic-off": "关闭麦克风 静音", "flash-off": "关闭闪光灯",
    "checkmark-done": "已读 双对勾", "ellipsis-horizontal": "更多 横向省略号", "ellipsis-vertical": "更多 竖向省略号",
    "reorder-two": "两条横线 拖动", "reorder-three": "三条横线 菜单", "reorder-four": "四条横线 拖动",
    "return-down-back": "回车 换行", "swap-horizontal": "左右交换 切换", "swap-vertical": "上下交换 排序",
    "trending-up": "上涨 趋势", "trending-down": "下跌 趋势", "thumbs-up": "点赞 赞", "thumbs-down": "点踩 差评",
    "play-skip-forward": "下一首", "play-skip-back": "上一首", "play-forward": "快进", "play-back": "快退",
    "arrow-undo": "撤销", "arrow-redo": "重做 转发", "calendar-number": "日历 日期", "bag-handle": "手提袋 购物",
    "document-lock": "加密文档", "people-circle": "群组 头像", "person-circle": "头像 个人", "person-add": "添加联系人 加好友",
    "person-remove": "删除联系人", "mail-unread": "未读邮件", "mail-open": "已读邮件", "folder-open": "打开文件夹",
    "cloud-download": "云端下载", "cloud-upload": "云端上传", "cloud-done": "已同步 云端完成",
}

BRANDS = {
    "alipay": "支付宝", "amazon": "亚马逊", "android": "安卓", "apple": "苹果", "apple-appstore": "苹果 应用商店",
    "chrome": "谷歌浏览器", "edge": "微软浏览器", "firefox": "火狐", "google": "谷歌", "google-playstore": "谷歌 应用商店",
    "microsoft": "微软", "wechat": "微信", "windows": "微软", "youtube": "视频", "bitcoin": "比特币", "euro": "欧元",
    "usd": "美元", "yen": "日元 人民币", "no-smoking": "禁止吸烟", "closed-captioning": "字幕", "rss": "订阅",
    "tiktok": "抖音", "x": "推特", "twitter": "推特", "playstation": "游戏机", "xbox": "游戏机", "github": "代码托管",
    "whatsapp": "聊天", "discord": "聊天", "slack": "聊天", "skype": "通话", "paypal": "支付", "venmo": "支付",
}


def main():
    index, out = Path(sys.argv[1]), Path(sys.argv[2])
    lines = []
    missing = set()
    for line in index.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        name = line.split("\t")[0]
        words = []
        if name.startswith("logo-"):
            brand = name[len("logo-"):]
            words += ["品牌", "标志"] + BRANDS.get(brand, "").split()
        else:
            words += PHRASES.get(name, "").split()
            for w in name.split("-"):
                if w in WORDS:
                    words += WORDS[w].split()
                else:
                    missing.add(w)
        seen = []
        for w in words:
            if w and w not in seen:
                seen.append(w)
        lines.append(f"{name}\t{' '.join(seen)}")
    if missing:
        raise SystemExit(f"缺翻译：{sorted(missing)}")
    out.write_text("# 中文搜索关键词\n" + "\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    print(len(lines))


if __name__ == "__main__":
    main()

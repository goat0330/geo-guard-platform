const _cb = (cb, result, params) => {
  cb && cb(params)
  return result
}

// 手机号正则
export const phoneReg = /^1[3-9]\d{9}$/
// 邮箱正则
export const emailReg = /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/
// 密码强度验证
const weakReg = /^(?=.*[a-zA-Z0-9!@#$%^&*(),.?":{}|<>])[a-zA-Z0-9!@#$%^&*(),.?":{}|<>]{8,}$/;
const mediumReg =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)|(?=.*[a-z])(?=.*[!@#$%^&*(),.?":{}|<>])|(?=.*[A-Z])(?=.*[!@#$%^&*(),.?":{}|<>])|(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>])[a-zA-Z0-9!@#$%^&*(),.?":{}|<>]{10,}$/;
const strongReg = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>])[a-zA-Z0-9!@#$%^&*(),.?":{}|<>]{8,}$/

// 校验手机是否合法
export const validatePhone = (rule, value, cb) => {
  if (!value) {
    return _cb(cb, false, '手机号不能为空')
  }
  if (!phoneReg.test(value)) {
    return _cb(cb, false, '手机号不合法')
  }
  return _cb(cb, true)
}

// 校验邮箱是否合法
export const validateEmail = (rule, value, cb) => {
  if (!value) {
    return cb(new Error('邮箱不能为空'))
  }
  if (!emailReg.test(value)) {
    return cb(new Error('邮箱不合法'))
  }
  cb()
}

// 校验用户名格式，用户名支持英文、数字和连字符
export const validateUsername = (rule, value, cb) => {
  if (!value) {
    return cb(new Error('用户名不能为空'))
  }
  const reg = /^[a-zA-Z0-9-]+$/
  if (!reg.test(value)) {
    return cb(new Error('用户名需为英文、数字和连字符'))
  }
  cb()
}

// // 校验两次密码是否一致
export const validateConfirmPwd = (pwd1, pwd2, cb) => {
  const _cb = (result, params) => {
    cb(params)
    return result
  }
  if (!pwd1) {
    return _cb(false, new Error('请输入密码'))
  }

  if (pwd2 && pwd1 !== pwd2) {
    return _cb(false, new Error('两次密码不一致'))
  }
  if (!strongReg.test(pwd1)) {
    return _cb(false, '密码强度低，密码为8位以上包含大小写字母、数字和指定的特殊字符')
  }
  return _cb(true)
}

// 真实姓名验证，中文（2-4个字符）或英文（50个以内）
export const validateRealName = (rule, value, cb) => {
  if (!value) {
    return cb(new Error('真实姓名不能为空'))
  }
  const reg = /^(?:[\u4e00-\u9fa5]{2,20}|[a-zA-Z]+(?: [a-zA-Z]+){0,49})$/
  if (!reg.test(value)) {
    return cb(new Error('真实姓名需为中文（2-20个字符）或英文全名（50个以内）'))
  }
  cb()
}

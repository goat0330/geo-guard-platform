<script lang="jsx">
import { ref, h, resolveComponent, reactive, watch, onMounted, unref } from 'vue'
export default {
  props: {
    formList: {
      type: Array,
      default: () => [],
    },
    // 父组件必须用ref定义
    formData: {
      type: Object,
      default: () => ({}),
    },
    labelWidth: {
      type: [String, Number],
      default: 120,
    },
    submitText: {
      type: String,
      default: '保存',
    },
    resetText: {
      type: String,
      default: '重置',
    },
    btnInForm: {
      type: Boolean,
      default: false,
    },
    noOperateBtn: {
      type: Boolean,
      default: false,
    },
    noResetBtn: {
      type: Boolean,
      default: false,
    },
    operateBtnFixedCenter: {
      type: Boolean,
      default: true,
    },
    defaultColSpan: {
      type: [Number, null],
      default: null,
    },
    colProps: {
      type: Object,
      default: () => ({}),
    },
    resetFn: {
      type: Function,
    },
    noLayout: {
      type: Boolean,
      default: false,
    },
    changeEmitSearch: {
      type: Boolean,
      default: false,
    },
    labelPosition: {
      type: String,
      default: 'right',
    },
  },
  emits: ['update:formData', 'submit'],
  setup(props, { emit, expose }) {
    const ElForm = resolveComponent('ElForm')
    const ElFormItem = resolveComponent('ElFormItem')
    const ElRow = resolveComponent('ElRow')
    const ElCol = resolveComponent('ElCol')
    const ElButton = resolveComponent('ElButton')

    // const ElInput = resolveComponent('ElInput')

    const CompObj = {}
    props.formList.forEach((item) => {
      if (item.type) {
        CompObj[item.type] = resolveComponent(`El${item.type}`)
      }
    })

    function simpleClone(val) {
      if (typeof val !== 'object') {
        return val
      }
      return JSON.parse(JSON.stringify(val))
    }
    function isExist(val) {
      return val || val == 0
    }
    function getDefaultValue(type) {
      let defaultValue = null
      const defaultValueArrayItems = ['CheckboxGroup', 'daterange']
      const defaultValueFalseItems = ['switch']
      const defaultValueNumberItems = ['Slider', 'InputNumber', 'Rate']

      if (defaultValueArrayItems.indexOf(type) > -1) {
        defaultValue = []
      } else if (defaultValueFalseItems.indexOf(type) > -1) {
        defaultValue = false
      } else if (defaultValueNumberItems.indexOf(type) > -1) {
        defaultValue = 0
      } else {
        defaultValue = ''
      }
      return defaultValue
    }
    function setDefaultForm() {
      let _formData = {}
      props.formList.forEach((item) => {
        _formData[item.key] = item.defaultValue || getDefaultValue(item.type)
      })

      return _formData
    }

    const innerFormData = ref(setDefaultForm())
    onMounted(() => {
      emit('update:formData', simpleClone(innerFormData.value))
    })
    watch(
      () => props.formData,
      (val) => {
        let _val = simpleClone(val)
        innerFormData.value = simpleClone(_val)
      },
      {
        deep: true,
        immediate: true,
      },
    )
    const formRef = ref()
    function renderForm(h) {
      return h(
        ElForm,
        {
          ref: formRef,
          model: innerFormData,
          class: 'r-form__content',
          labelWidth: typeof props.labelWidth === 'string' ? props.labelWidth : `${props.labelWidth}px`,
          labelPosition: props.labelPosition,
        },
        { default: () => renderFormItem(h) },
      )
    }

    function renderFormItem(h) {
      // let formItems = simpleClone(props.formList)
      let formItems = [
        ...props.formList,
        // {
        //   title: '',
        //   span: 6,
        //   render: (h) =>
        //     h(
        //       'div',
        //       { class: 'operate-btn-in-form' },
        //       {
        //         default: () => {
        //           return [h(ElButton, {}, { default: () => '查询' }), h(ElButton, {}, { default: () => '重置' })]
        //         },
        //       },
        //     ),
        // },
      ]
      return h(
        ElRow,
        {},
        {
          default: () =>
            formItems.map((item) => {
              if (item.hidden) {
                return null
              }
              const formItem = h(
                ElFormItem,
                {
                  label: item.title ? item.title + ' :' : '',
                  prop: item.key || '',
                  ...(isExist(item.labelWidth) && {
                    'label-width': item.labelWidth + 'px',
                  }),
                  ...(props.noLayout && {
                    // 'label-width': 'auto',
                    style: {
                      width: item.width
                        ? typeof item.width === 'number'
                          ? item.width / 16 + 'rem'
                          : item.width
                        : 'auto',
                    },
                    class: {
                      'no-layout': true,
                    },
                  }),
                  rules: item.rules || [],

                  ...item.itemProps,
                },
                { default: () => renderFormItemContent(h, item) },
              )
              return props.noLayout
                ? formItem
                : h(
                    ElCol,
                    {
                      span: item.span || props.defaultColSpan || 24,
                      ...props.colProps,
                    },
                    {
                      default: () => formItem,
                    },
                  )
            }),
        },
      )
    }
    function renderFormItemContent(h, item) {
      // 渲染当前表单项
      if (item.render) {
        return item.render(h)
      }
      const emitFormDataUpdate = () => {
        const _val = simpleClone(innerFormData.value)
        emit('update:formData', reactive(_val))
        return _val
      }
      return h(
        CompObj[item.type],
        {
          modelValue: innerFormData.value[item.key],
          'onUpdate:modelValue': (val) => {
            innerFormData.value[item.key] = val
            emitFormDataUpdate()
          },
          autocomplete: 'new-password',
          ...item.props,
          onChange: () => {
            let _val = emitFormDataUpdate()
            item.onChange && item.onChange(_val)
            props.changeEmitSearch && emit('submit', _val)
          },
        },
        {
          default: () => (item.options ? renderOptions(h, item) : null),
        },
      )
    }

    // 需要渲染子元素的表单项
    let childrenTagEnum = {
      CheckboxGroup: 'ElCheckbox',
      RadioGroup: 'ElRadio',
      Select: 'ElOption',
      SelectV2: 'ElOption',
    }
    function renderOptions(h, item) {
      const childElComp = resolveComponent(childrenTagEnum[item.type])

      return item.options.map((item, key) => {
        return h(childElComp, { label: item.label, value: item.value, key: key, ...item.props })
      })
    }

    // 渲染操作按钮
    function renderSubmitBtn(h) {
      if (props.noOperateBtn) return
      return h(
        'div',
        {
          class: 'r-form__operate-btn',
          style: {
            'text-align': props.operateBtnFixedCenter ? 'center' : 'left',
          },
        },
        {
          default: () => [
            h(ElButton, { onClick: handleSubmit, type: 'primary' }, { default: () => props.submitText }),
            props.noResetBtn ? null : h(ElButton, { onClick: handleReset }, { default: () => props.resetText }),
          ],
        },
      )
    }
    // 触发查询或者提交
    function handleSubmit() {
      emit('submit', unref(innerFormData))
    }
    // 触发重置
    function handleReset() {
      if (props.resetFn) {
        props.resetFn()
        return
      }
      let data = setDefaultForm()
      innerFormData.value = data
      emit('update:formData', data)
    }
    // 校验
    function validate() {
      return formRef.value.validate()
    }
    // 校验某项
    function validateField(key) {
      return formRef.value.validateField(key)
    }
    // 清楚校验
    function clearValidate(key) {
      return formRef.value.clearValidate(key)
    }
    // 清楚某项的校验
    function resetFields(keys) {
      return formRef.value.resetFields(Array.isArray(keys) ? keys : [keys])
    }

    expose({
      clearValidate,
      validate,
      resetFields,
      validateField,
      handleReset,
    })

    return () =>
      h(
        'div',
        {
          class: 'r-form-box',
        },
        [renderForm(h), renderSubmitBtn(h)],
      )
  },
}
</script>

<style scoped lang="less">
// @import './customize.less';

.r-form-box {
  --el-border-radius-base: 4px;
  :deep(.r-form__content) {
    .el-input-number,
    .el-cascader,
    .el-autocomplete,
    .el-input,
    .el-select {
      width: 100%;
      // min-width: 280px;
    }
    .el-form-item {
      margin-right: 20px;
    }
    .el-form-item__label-wrap {
      margin-left: 0 !important;
      margin-right: 0 !important;
    }
    .operate-btn-in-form {
      width: 100%;
      text-align: right;
    }
    .el-select__caret {
      color: #666666;
    }
    .el-select__placeholder {
      // color: #202020!important;
    }
    .el-select__placeholder.is-transparent {
      // color: var(--el-text-color-placeholder);
    }
  }
}
</style>

<template>
  <section class="account-page">
    <div class="account-content">
      <p class="section-label">账号设置</p>
      <h1>个人信息</h1>
      <article v-loading="loading" class="profile-card">
        <header class="profile-header">
          <div class="avatar-settings">
            <AccountAvatar :username="username" :src="avatar" :size="52" />
            <label class="avatar-picker">
              <span>更换头像</span>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                :disabled="savingAvatar"
                @change="changeAvatar"
              />
            </label>
          </div>
          <div class="identity">
            <h2>{{ profile?.realName || username }}</h2>
            <span>{{ username }}</span>
          </div>
          <el-button :icon="Edit" plain type="primary" :disabled="!profile" @click="openEditor">编辑资料</el-button>
        </header>
        <section class="contact-info">
          <h3>联系方式</h3>
          <dl>
            <div><dt><el-icon><Iphone /></el-icon>手机号</dt><dd>{{ maskedPhone }}</dd></div>
            <div><dt><el-icon><Message /></el-icon>邮箱</dt><dd>{{ maskedMail }}</dd></div>
          </dl>
        </section>
      </article>
    </div>

    <el-dialog v-model="dialogVisible" title="编辑资料" width="min(440px, 92vw)" destroy-on-close>
      <el-form ref="editor" :model="draft" :rules="formRule" label-width="72px" @submit.prevent="saveProfile">
        <el-form-item label="用户名"><el-input :model-value="username" disabled /></el-form-item>
        <el-form-item label="姓名" prop="realName"><el-input v-model="draft.realName" clearable /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="draft.phone" type="tel" clearable /></el-form-item>
        <el-form-item label="邮箱" prop="mail"><el-input v-model="draft.mail" clearable /></el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="draft.password" type="password" show-password autocomplete="new-password" placeholder="留空保留当前密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, getCurrentInstance, inject, ref } from 'vue'
import { Edit, Iphone, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AccountAvatar from '@/components/AccountAvatar.vue'
import { getUsername } from '@/core/auth'

const { proxy } = getCurrentInstance()
const { avatar, saveAvatar } = inject('accountAvatar')
const profile = ref(null)
const draft = ref({})
const editor = ref()
const dialogVisible = ref(false)
const loading = ref(false)
const saving = ref(false)
const savingAvatar = ref(false)
const username = computed(() => profile.value?.username || getUsername() || '')
const maskedPhone = computed(() => {
  const phone = profile.value?.phone
  if (!phone) return '—'
  return /^\d{11}$/.test(phone) ? phone.slice(0, 3) + ' **** ' + phone.slice(-4) : '****'
})
const maskedMail = computed(() => {
  const mail = profile.value?.mail
  if (!mail) return '—'
  const at = mail.lastIndexOf('@')
  if (at < 1) return '****'
  return mail.slice(0, Math.min(3, at - 1)) + '****' + mail.slice(at)
})

const formRule = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  mail: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }
  ],
  password: [{ min: 8, max: 15, message: '密码长度为 8 至 15 位', trigger: 'blur' }]
}

async function loadProfile() {
  loading.value = true
  try {
    const { data } = await proxy.$API.user.queryUserInfo(getUsername())
    if (!data?.data) throw new Error(data?.message || '个人信息读取失败')
    profile.value = data.data
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function openEditor() {
  draft.value = { ...profile.value, password: '' }
  dialogVisible.value = true
}

async function changeAvatar(event) {
  const input = event.currentTarget
  const file = input.files[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 4 * 1024 * 1024) {
    ElMessage.warning('请选择 4 MB 以内的 JPG、PNG 或 WebP 图片')
    return
  }

  savingAvatar.value = true
  try {
    const image = await createImageBitmap(file)
    const side = Math.min(image.width, image.height)
    const canvas = document.createElement('canvas')
    canvas.width = canvas.height = 192
    const context = canvas.getContext('2d')
    context.fillStyle = '#ffffff'
    context.fillRect(0, 0, canvas.width, canvas.height)
    context.drawImage(image, (image.width - side) / 2, (image.height - side) / 2, side, side, 0, 0, canvas.width, canvas.height)
    image.close()
    await saveAvatar(canvas.toDataURL('image/jpeg', 0.85))
    ElMessage.success('头像已更新')
  } catch {
    ElMessage.error('头像更新失败，请换一张图片后重试')
  } finally {
    savingAvatar.value = false
  }
}

async function saveProfile() {
  if (saving.value || !await editor.value?.validate().catch(() => false)) return
  saving.value = true
  try {
    const { data } = await proxy.$API.user.editUser(draft.value)
    if (data?.code !== '0') throw new Error(data?.message || '保存失败')
    dialogVisible.value = false
    ElMessage.success('修改成功')
    await loadProfile()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

loadProfile()
</script>

<style scoped>
.account-page {
  box-sizing: border-box;
  min-height: calc(100vh - 54px);
  height: auto;
  padding: 32px 24px;
  background: #eef1f6;
  color: #202b3c;
}
.account-content { max-width: 880px; margin: 0 auto; }
.section-label { margin: 0 0 6px; font-size: 13px; color: #586679; }
.account-content h1 { margin: 0 0 20px; font-size: 20px; font-weight: 600; }
.profile-card { background: white; border: 1px solid #dce2eb; border-radius: 8px; }
.profile-header { display: flex; align-items: center; gap: 14px; padding: 24px; }
.avatar-settings { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.avatar-picker { position: relative; font-size: 12px; color: #2465ad; cursor: pointer; }
.avatar-picker:focus-within { outline: 2px solid #2465ad; outline-offset: 2px; }
.avatar-picker input { position: absolute; inset: 0; width: 100%; opacity: 0; cursor: inherit; }
.avatar-picker:has(input:disabled) { opacity: 0.6; cursor: wait; }
.identity { min-width: 0; flex: 1; overflow-wrap: anywhere; }
.identity h2 { margin: 0 0 4px; font-size: 17px; font-weight: 600; }
.identity span { font-size: 14px; color: #626f80; }
.contact-info { margin: 0 24px; padding: 20px 0 8px; border-top: 1px solid #e3e8ef; }
.contact-info h3 { margin: 0 0 8px; font-size: 15px; font-weight: 600; }
.contact-info dl { margin: 0; font-size: 14px; }
.contact-info dl > div { display: grid; grid-template-columns: minmax(92px, 20%) 1fr; gap: 16px; padding: 15px 0; }
.contact-info dl > div + div { border-top: 1px solid #edf0f4; }
.contact-info dt { display: flex; align-items: center; gap: 10px; color: #586679; }
.contact-info dd { min-width: 0; margin: 0; overflow-wrap: anywhere; }
@media (max-width: 600px) {
  .account-page { padding: 20px 12px; }
  .profile-header { padding: 16px; gap: 10px; }
  .contact-info { margin: 0 16px; }
}
</style>

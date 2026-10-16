import Cookies from 'js-cookie'

const TokenKey = 'token'

export function getToken() {
  return Cookies.get(TokenKey)
}
export function getUsername() {
  return Cookies.get('username')
}

export function setToken(token) {
  return Cookies.set(TokenKey, token, { sameSite: 'strict', secure: window.location.protocol === 'https:' })
}

export function setUsername(username) {
  return Cookies.set('username', username, { sameSite: 'strict', secure: window.location.protocol === 'https:' })
}

export function removeKey() {
  return Cookies.remove(TokenKey)
}

export function removeUsername() {
  return Cookies.remove('username')
}



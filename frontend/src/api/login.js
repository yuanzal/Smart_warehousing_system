import request from '@/utils/request'

export async function  login(requestData) {
    return request.post('/login', requestData, {
      headers: {
        'Admin-Token':window.localStorage.token,
        'Content-Type': 'application/json;charset=UTF-8'
      }
    })
}
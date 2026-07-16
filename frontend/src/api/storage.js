import axios from 'axios'

const BASE_URL = '/api/storage'

export const getSlots = () => {
    return axios.get(`${BASE_URL}/slots`)
}
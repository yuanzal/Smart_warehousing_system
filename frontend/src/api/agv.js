import axios from 'axios'

const BASE_URL = '/api/device'

export const getAgvPositions = () => {
    return axios.get(`${BASE_URL}/agv/positions`)
}
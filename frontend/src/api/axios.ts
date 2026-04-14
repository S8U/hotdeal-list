import Axios, { type AxiosRequestConfig } from "axios";

export const AXIOS_INSTANCE = Axios.create({
    baseURL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080",
    paramsSerializer: {
        indexes: null, // platforms=A&platforms=B (Spring 호환, brackets 제거)
    },
});

export const customInstance = <T>(config: AxiosRequestConfig): Promise<T> => {
    const source = Axios.CancelToken.source();
    const promise = AXIOS_INSTANCE({ ...config, cancelToken: source.token }).then(
        ({ data }) => data,
    );

    // @ts-expect-error allow attaching cancel for react-query
    promise.cancel = () => source.cancel("Query was cancelled");

    return promise;
};

export default customInstance;

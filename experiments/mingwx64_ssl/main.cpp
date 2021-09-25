#pragma clang diagnostic ignored "-Wwritable-strings"
#pragma ide diagnostic ignored "OCUnusedMacroInspection"
#pragma ide diagnostic ignored "UnusedValue"
#pragma ide diagnostic ignored "modernize-use-nullptr"
#pragma ide diagnostic ignored "cppcoreguidelines-narrowing-conversions"

#define SECURITY_WIN32 1

#include <winsock2.h>
#include <sspi.h>
#include <schannel.h>
#include <cstdio>
#include <commctrl.h>
#include <cstring>
#include <tchar.h>
#include <cassert>
#include <ntdef.h>
#include <cmath>
#include <algorithm>

#define SET_SSL_BUFFER(buffer, type, count, pv) buffer.BufferType = type; buffer.cbBuffer = count; buffer.pvBuffer = pv;


template<class T>
class GB {
private:

    T *buffer;
    unsigned int size;

public:

    GB(int s = 0) {
        if (!s) s = 1;
        buffer = new T[s];
        memset(buffer, 0, s * sizeof(T));
        size = s;
    }

    ~GB() { delete[] buffer; }

    operator T *() { return buffer; }

    void clear() { ZeroMemory(buffer, size * sizeof(T)); }

    void Resize(unsigned int news) {
        if (news == size) return;
        T *newd = new T[news];
        int newbs = news * sizeof(T);
        ZeroMemory((void *) newd, newbs);
        memcpy((void *) newd, buffer, std::min(news, size) * sizeof(T));
        delete[] buffer;
        buffer = newd;
        size = news;
    }
};

class SSL_SOCKET {
public:

    SSL_SOCKET(SOCKET);

    void SetDestinationName(TCHAR *n);

    int ClientNegotiate();

    ~SSL_SOCKET();

    int s_ssend(char *b, int sz);

    int s_recv(char *b, int sz);

    int ssend_p(char *b, int sz) const;

    int recv_p(char *b, int sz) const;

    int ClientOff();


private:
    SOCKET X;
    HCERTSTORE hCS;
    SCHANNEL_CRED m_SchannelCred;
    CredHandle hCred;
    CtxtHandle hCtx;
    TCHAR dn[1000];
    SecBufferDesc sbin;
    SecBufferDesc sbout;
    bool InitContext;
    GB<char> ExtraData;
    int ExtraDataSize;
    GB<char> PendingRecvData;
    int PendingRecvDataSize;
};

SSL_SOCKET::SSL_SOCKET(SOCKET x) {
    X = x;
    hCS = 0;
    hCred.dwLower = 0;
    hCred.dwUpper = 0;
    hCtx.dwLower = 0;
    hCtx.dwUpper = 0;
    memset(dn, 0, 1000 * sizeof(TCHAR));
    InitContext = false;
    ExtraDataSize = 0;
    PendingRecvDataSize = 0;
    memset(&m_SchannelCred, 0, sizeof(m_SchannelCred));
    m_SchannelCred.dwVersion = SCHANNEL_CRED_VERSION;
    m_SchannelCred.dwFlags = SCH_CRED_NO_DEFAULT_CREDS | SCH_CRED_NO_SYSTEM_MAPPER | SCH_CRED_REVOCATION_CHECK_CHAIN;
    SECURITY_STATUS ss = AcquireCredentialsHandle(0, SCHANNEL_NAME, SECPKG_CRED_OUTBOUND, 0, NULL, 0, 0, &hCred, 0);
    assert(!FAILED(ss));
}

SSL_SOCKET::~SSL_SOCKET() {
    ClientOff();
    if (hCtx.dwLower || hCtx.dwLower) DeleteSecurityContext(&hCtx);
    if (hCred.dwLower || hCred.dwLower) FreeCredentialHandle;
    if (hCS) CertCloseStore(hCS, 0);
    hCS = 0;
}


void SSL_SOCKET::SetDestinationName(TCHAR *n) {
    _tcscpy(dn, n);
}

int SSL_SOCKET::ClientOff() {
    // Client wants to disconnect

    SECURITY_STATUS ss;
    SecBuffer OutBuffers[1];
    DWORD dwType = SCHANNEL_SHUTDOWN;

    SET_SSL_BUFFER(OutBuffers[0], SECBUFFER_TOKEN, sizeof(dwType), &dwType)
    sbout.cBuffers = 1;
    sbout.pBuffers = OutBuffers;
    sbout.ulVersion = SECBUFFER_VERSION;

    while (true) {
        ss = ApplyControlToken(&hCtx, &sbout);
        if (FAILED(ss)) return -1;

        DWORD dwSSPIFlags;
        DWORD dwSSPIOutFlags;
        dwSSPIFlags =
                ISC_REQ_SEQUENCE_DETECT | ISC_REQ_REPLAY_DETECT | ISC_REQ_CONFIDENTIALITY | ISC_RET_EXTENDED_ERROR |
                ISC_REQ_ALLOCATE_MEMORY | ISC_REQ_STREAM;

        SET_SSL_BUFFER(OutBuffers[0], SECBUFFER_TOKEN, 0, 0)
        sbout.cBuffers = 1;
        sbout.pBuffers = OutBuffers;
        sbout.ulVersion = SECBUFFER_VERSION;

        ss = InitializeSecurityContext(
                &hCred, &hCtx, NULL, dwSSPIFlags, 0, SECURITY_NATIVE_DREP, NULL, 0, &hCtx,
                &sbout, &dwSSPIOutFlags, 0
        );
        if (FAILED(ss))
            return -1;

        PBYTE pbMessage;
        DWORD cbMessage;
        pbMessage = (BYTE *) (OutBuffers[0].pvBuffer);
        cbMessage = OutBuffers[0].cbBuffer;

        if (pbMessage != NULL && cbMessage != 0) {
            int rval = ssend_p((char *) pbMessage, cbMessage);
            FreeContextBuffer(pbMessage);
            return rval;
        }
        break;
    }
    return 1;
}

int SSL_SOCKET::ssend_p(char *b, int sz) const {
    // same as send, but forces reading ALL sz
    int rs = 0;
    while (true) {
        int rval = send(X, b + rs, sz - rs, 0);
        if (rval == 0 || rval == SOCKET_ERROR) return rs;
        rs += rval;
        if (rs == sz) return rs;
    }
}

int SSL_SOCKET::recv_p(char *b, int sz) const { return recv(X, b, sz, 0); }


int SSL_SOCKET::s_recv(char *b, int sz) {
    SecPkgContext_StreamSizes Sizes;

    retry:
    SECURITY_STATUS ss = QueryContextAttributes(&hCtx, SECPKG_ATTR_STREAM_SIZES, &Sizes);
    if (FAILED(ss)) {
        if (ss == SEC_E_INVALID_HANDLE) {
            ClientNegotiate();
            goto retry;
        }

        return -1;
    }

    int TotalR = 0;
    int pI = 0;
    SecBuffer Buffers[5] = {0};
    SecBuffer *pDataBuffer;
    SecBuffer *pExtraBuffer;
    GB<char> mmsg(Sizes.cbMaximumMessage * 10);

    if (PendingRecvDataSize) {
        if (sz <= PendingRecvDataSize) {
            memcpy(b, PendingRecvData, sz);
            GB<char> dj(PendingRecvDataSize);
            memcpy(dj, PendingRecvData, PendingRecvDataSize);
            memcpy(PendingRecvData, dj + sz, PendingRecvDataSize - sz);
            PendingRecvDataSize -= sz;
            return sz;
        }
        memcpy(b, PendingRecvData, PendingRecvDataSize);
        sz = PendingRecvDataSize;
        PendingRecvDataSize = 0;
        return sz;
    }

    while (true) {
        unsigned int dwMessage = Sizes.cbMaximumMessage;

        if (dwMessage > Sizes.cbMaximumMessage) dwMessage = Sizes.cbMaximumMessage;

        if (ExtraDataSize) {
            memcpy(mmsg + pI, ExtraData, ExtraDataSize);
            pI += ExtraDataSize;
            ExtraDataSize = 0;
        } else {
            int rval = recv_p(mmsg + pI, dwMessage);
            if (rval == 0 || rval == -1) return rval;
            pI += rval;
        }

        SET_SSL_BUFFER(Buffers[0], SECBUFFER_DATA, pI, mmsg)
        SET_SSL_BUFFER(Buffers[1], SECBUFFER_EMPTY, 0, NULL)
        SET_SSL_BUFFER(Buffers[2], SECBUFFER_EMPTY, 0, NULL)
        SET_SSL_BUFFER(Buffers[3], SECBUFFER_EMPTY, 0, NULL)

        sbin.ulVersion = SECBUFFER_VERSION;
        sbin.pBuffers = Buffers;
        sbin.cBuffers = 4;

        ss = DecryptMessage(&hCtx, &sbin, 0, NULL);
        if (ss == SEC_E_INCOMPLETE_MESSAGE) continue;
        if (ss != SEC_E_OK && ss != SEC_I_RENEGOTIATE && ss != SEC_I_CONTEXT_EXPIRED) return -1;

        pDataBuffer = NULL;
        pExtraBuffer = NULL;
        for (int i = 0; i < 4; i++) {
            if (pDataBuffer == NULL && Buffers[i].BufferType == SECBUFFER_DATA) pDataBuffer = &Buffers[i];
            if (pExtraBuffer == NULL && Buffers[i].BufferType == SECBUFFER_EXTRA) pExtraBuffer = &Buffers[i];
        }
        if (pExtraBuffer) {
            ExtraDataSize = pExtraBuffer->cbBuffer;
            ExtraData.Resize(ExtraDataSize + 10);
            memcpy(ExtraData, pExtraBuffer->pvBuffer, ExtraDataSize);
            pI = 0;
        }

        if (ss == SEC_I_RENEGOTIATE) {
            ss = ClientNegotiate();
            if (FAILED(ss)) return -1;
        }


        if (pDataBuffer == 0) break;

        TotalR = pDataBuffer->cbBuffer;
        if (TotalR <= sz) {
            memcpy(b, pDataBuffer->pvBuffer, TotalR);
        } else {
            TotalR = sz;
            memcpy(b, pDataBuffer->pvBuffer, TotalR);
            PendingRecvDataSize = pDataBuffer->cbBuffer - TotalR;
            PendingRecvData.Resize(PendingRecvDataSize + 100);
            PendingRecvData.clear();
            memcpy(PendingRecvData, (char *) pDataBuffer->pvBuffer + TotalR, PendingRecvDataSize);
        }

        break;
    }


    return TotalR;
}

int SSL_SOCKET::s_ssend(char *b, int sz) {
    SecPkgContext_StreamSizes Sizes;

    retry:
    SECURITY_STATUS ss = QueryContextAttributes(&hCtx, SECPKG_ATTR_STREAM_SIZES, &Sizes);
    if (FAILED(ss)) {
        if (ss == SEC_E_INVALID_HANDLE) {
            ClientNegotiate();
            goto retry;
        }

        return -1;
    }

    SecBuffer Buffers[4];
    int mPos = 0;
    while (true) {
        GB<char> mmsg(Sizes.cbMaximumMessage * 2);
        GB<char> mhdr(Sizes.cbHeader * 2);
        GB<char> mtrl(Sizes.cbTrailer * 2);

        unsigned int dwMessage = sz - mPos;
        if (dwMessage == 0)
            break; // all ok!

        if (dwMessage > Sizes.cbMaximumMessage) {
            dwMessage = Sizes.cbMaximumMessage;
        }
        memcpy(mmsg, b + mPos, dwMessage);
        mPos += dwMessage;


        SET_SSL_BUFFER(Buffers[0], SECBUFFER_STREAM_HEADER, Sizes.cbHeader, mhdr)
        SET_SSL_BUFFER(Buffers[1], SECBUFFER_DATA, dwMessage, mmsg)
        SET_SSL_BUFFER(Buffers[2], SECBUFFER_STREAM_TRAILER, Sizes.cbTrailer, mtrl)
        SET_SSL_BUFFER(Buffers[3], SECBUFFER_EMPTY, 0, 0)
        sbin.ulVersion = SECBUFFER_VERSION;
        sbin.pBuffers = Buffers;
        sbin.cBuffers = 4;

        ss = EncryptMessage(&hCtx, 0, &sbin, 0);
        if (FAILED(ss)) return -1;


        // Send this message
        int rval;
        rval = ssend_p((char *) Buffers[0].pvBuffer, Buffers[0].cbBuffer);
        if (rval != Buffers[0].cbBuffer) return rval;
        rval = ssend_p((char *) Buffers[1].pvBuffer, Buffers[1].cbBuffer);
        if (rval != Buffers[1].cbBuffer) return rval;
        rval = ssend_p((char *) Buffers[2].pvBuffer, Buffers[2].cbBuffer);
        if (rval != Buffers[2].cbBuffer) return rval;
    }

    return sz;
}


int SSL_SOCKET::ClientNegotiate() {
    SECURITY_STATUS ss = SEC_I_CONTINUE_NEEDED;
    GB<char> t(0x11000);
    SecBuffer bufsi[2];
    SecBuffer bufso[1];
    int pt = 0;

    // Loop using InitializeSecurityContext until success
    while (true) {
        if (ss != SEC_I_CONTINUE_NEEDED && ss != SEC_E_INCOMPLETE_MESSAGE && ss != SEC_I_INCOMPLETE_CREDENTIALS)
            break;

        DWORD dwSSPIFlags =
                ISC_REQ_SEQUENCE_DETECT | ISC_REQ_REPLAY_DETECT | ISC_REQ_CONFIDENTIALITY |
                ISC_RET_EXTENDED_ERROR | ISC_REQ_ALLOCATE_MEMORY | ISC_REQ_STREAM | ISC_REQ_MANUAL_CRED_VALIDATION;

        if (InitContext == 0) {
            // Initialize sbout
            SET_SSL_BUFFER(bufso[0], SECBUFFER_TOKEN, 0, 0)
            sbout.ulVersion = SECBUFFER_VERSION;
            sbout.cBuffers = 1;
            sbout.pBuffers = bufso;
        } else {
            // Get Some data from the remote site

            // Add also extradata?
            if (ExtraDataSize) {
                memcpy(t, ExtraData, ExtraDataSize);
                pt += ExtraDataSize;
                ExtraDataSize = 0;
            }


            int rval = recv(X, t + pt, 0x10000, 0);
            if (rval == 0 || rval == -1) return rval;
            pt += rval;

            // Put this data into the buffer so InitializeSecurityContext will do

            SET_SSL_BUFFER(bufsi[0], SECBUFFER_TOKEN, pt, t)
            SET_SSL_BUFFER(bufsi[1], SECBUFFER_EMPTY, 0, 0)
            sbin.ulVersion = SECBUFFER_VERSION;
            sbin.pBuffers = bufsi;
            sbin.cBuffers = 2;

            SET_SSL_BUFFER(bufso[0], SECBUFFER_TOKEN, 0, 0)
            sbout.cBuffers = 1;
            sbout.pBuffers = bufso;
            sbout.ulVersion = SECBUFFER_VERSION;
        }

        DWORD dwSSPIOutFlags = 0;

        SEC_E_INTERNAL_ERROR;
        ss = InitializeSecurityContext(
                &hCred,
                InitContext ? &hCtx : NULL,
                dn,
                dwSSPIFlags,
                0,
                0,//SECURITY_NATIVE_DREP,
                InitContext ? &sbin : NULL,
                0,
                InitContext ? NULL : &hCtx,
                &sbout,
                &dwSSPIOutFlags,
                NULL
        );

        if (ss == SEC_E_INCOMPLETE_MESSAGE) continue; // allow more

        pt = 0;

        if (FAILED(ss)) return -1;

        if (InitContext == 0 && ss != SEC_I_CONTINUE_NEEDED) return -1;

        if (!InitContext) {
            int rval = ssend_p((char *) bufso[0].pvBuffer, bufso[0].cbBuffer);
            FreeContextBuffer(bufso[0].pvBuffer);
            if (rval != bufso[0].cbBuffer) return -1;
            InitContext = true;
            continue;
        }

        // Pass data to the remote site
        int rval = ssend_p((char *) bufso[0].pvBuffer, bufso[0].cbBuffer);
        FreeContextBuffer(bufso[0].pvBuffer);
        if (rval != bufso[0].cbBuffer) return -1;

        if (ss == S_OK) break; // wow!!
    }
    return 0;
}

#define REQUEST_HOST "www.google.es"
//#define REQUEST_HOST "php.net"

int main() {
    int argc = 3;
    char *argv[] = {"program", REQUEST_HOST, "443"};
    SOCKET s;
    SSL_SOCKET *sx = 0;
    sockaddr_in dA, aa;
    int slen = sizeof(sockaddr_in);

    InitCommonControls();
    OleInitialize(0);
    WSADATA wData;
    WSAStartup(MAKEWORD(2, 2), &wData);
    printf("Tel 2.0 , Chourdakis Michael\r\n");
    if (argc < 2) {
        printf("Usage 1 : TEL <ip> <port>\r\n");
        printf("Usage 2 : TEL <port>\r\n");
        printf("Use * before the port to initiate a SSL session.\r\n");
        return 1;
    }
    char port[100] = {0};
    strcpy(port, argv[2]);

    printf("Mode 1 - connect to %s:%s...\r\n", argv[1], argv[2]);
    hostent *hp;

    memset(&dA, 0, sizeof(dA));
    dA.sin_family = AF_INET;
    unsigned long inaddr = inet_addr(argv[1]);
    if (inaddr != INADDR_NONE) {
        memcpy(&dA.sin_addr, &inaddr, sizeof(inaddr));
    } else {
        hp = gethostbyname(argv[1]);
        if (!hp) {
            printf("--- Remote system not found !\r\n");
            return 2;
        }
        memcpy(&dA.sin_addr, hp->h_addr, hp->h_length);
    }
    dA.sin_port = htons(atoi(port));
    s = socket(AF_INET, SOCK_STREAM, 0);
    if (connect(s, (sockaddr *) &dA, slen) < 0) {
        printf("--- Cannot connect !\r\n");
        return 3;
    }

    getpeername(s, (sockaddr *) &aa, &slen);

    printf("OK , connected with %s:%u...\r\n\r\n", inet_ntoa(aa.sin_addr), ntohs(aa.sin_port));
    sx = new SSL_SOCKET(s);
    sx->SetDestinationName(argv[1]);

    char *message = "GET / HTTP/1.1\r\nHost: " REQUEST_HOST "\r\nConnection: close\r\n\r\n";
    printf("%s\n", message);
    sx->s_ssend(message, strlen(message));

    while (true) {
        char c;
        int rval = sx->s_recv(&c, 1);
        if (rval == 0 || rval == -1) {
            printf("--- Disconnected !\r\n\r\n");
            exit(0);
        }
        putc(c, stdout);
    }
}

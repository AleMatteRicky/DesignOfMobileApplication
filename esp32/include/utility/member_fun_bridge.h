#pragma once

/*
In CPP, each member function takes an implicit parameter: the pointer to the
object whose method is being called. In C, this is not the case, hence functions
taking a callback cannot get the reference of the object, which is required in
CPP. To overcome this problem, we can use a class offering the expected C-like
function that under the hood has a reference of the object.

Note. calling the methods of this class is not thread safe so two or more
threads operating concurrently may override the previous set object reference,
calling an unexpected method.
*/

template <typename Class, typename ReturnType, typename... Args>
class MemberFunctionBridge {
public:
    static ReturnType wrapper(Args... args) {
        assert(objInstance && "No instance provided");
        assert(member_func && "No member function provided");
        return (objInstance->*member_func)(args...);
    }

    static void setup(Class *inst, ReturnType (Class::*func)(Args...)) {
        objInstance = inst;
        member_func = func;
    }

private:
    static Class *objInstance;
    static ReturnType (Class::*member_func)(Args...);
};

template <typename T, typename U, typename... Args>
T *MemberFunctionBridge<T, U, Args...>::objInstance = nullptr;
template <typename T, typename U, typename... Args>
U (T::*MemberFunctionBridge<T, U, Args...>::member_func)(Args...) = nullptr;
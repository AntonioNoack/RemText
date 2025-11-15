package me.anno.remtext.colors.impl

import me.anno.remtext.colors.impl.CLikeLanguage.Companion.splitKeywords

enum class CLikeLanguageType {
    C, CPP, C_OR_CPP, JAVA, CSHARP, GLSL, HLSL, GO,
    JAVASCRIPT, JSON, KOTLIN, PYTHON, RUST,  SWIFT, ZIG,
    PHP;

    val keywords by lazy {
        when (this) {
            C -> "auto,break,case,char,const,continue,default,do,double,else,enum,extern,float,for,goto,if," +
                    "int,long,register,return,short,signed,sizeof,static,struct,switch,typedef,union,unsigned," +
                    "void,volatile,while,_Bool,_Complex,_Imaginary"

            CPP -> "alignas,alignof,and,and_eq,asm,auto,bitand,bitor,bool,break,case,catch,char,char8_t," +
                    "char16_t,char32_t,class,compl,const,const_cast,continue,decltype,default,delete,do,double," +
                    "dynamic_cast,else,enum,explicit,export,extern,false,float,for,friend,goto,if,inline,int,long," +
                    "mutable,namespace,new,noexcept,not,not_eq,nullptr,operator,or,or_eq,private,protected,public," +
                    "register,reinterpret_cast,return,short,signed,sizeof,static,static_assert,static_cast,struct," +
                    "switch,template,this,thread_local,throw,true,try,typedef,typeid,typename,union,unsigned,using," +
                    "virtual,void,volatile,wchar_t,while,xor,xor_eq"

            C_OR_CPP -> "auto,break,case,char,const,continue,default,do,double,else,enum,extern,float,for,goto,if," +
                    "int,long,register,return,short,signed,sizeof,static,struct,switch,typedef,union,unsigned," +
                    "void,volatile,while,_Bool,_Complex,_Imaginary," +

                    "alignas,alignof,and,and_eq,asm,auto,bitand,bitor,bool,break,case,catch,char,char8_t," +
                    "char16_t,char32_t,class,compl,const,const_cast,continue,decltype,default,delete,do,double," +
                    "dynamic_cast,else,enum,explicit,export,extern,false,float,for,friend,goto,if,inline,int,long," +
                    "mutable,namespace,new,noexcept,not,not_eq,nullptr,operator,or,or_eq,private,protected,public," +
                    "register,reinterpret_cast,return,short,signed,sizeof,static,static_assert,static_cast,struct," +
                    "switch,template,this,thread_local,throw,true,try,typedef,typeid,typename,union,unsigned,using," +
                    "virtual,void,volatile,wchar_t,while,xor,xor_eq"

            JAVA -> "abstract,assert,boolean,break,byte,case,catch,char,class,const,continue,default,do,double," +
                    "else,enum,extends,false,final,finally,float,for,if,implements,import,instanceof,int,interface," +
                    "long,native,new,null,package,private,protected,public,return,short,static,strictfp,super,switch," +
                    "synchronized,this,throw,throws,transient,true,try,void,volatile,while,var,yield,record,sealed," +
                    "permits,non-sealed"

            CSHARP -> "abstract,as,base,bool,break,byte,case,catch,char,checked,class,const,continue,decimal," +
                    "default,delegate,do,double,else,enum,event,explicit,extern,false,finally,fixed,float,for," +
                    "foreach,goto,if,implicit,in,int,interface,internal,is,lock,long,namespace,new,null,object," +
                    "operator,out,override,params,private,protected,public,readonly,ref,return,sbyte,sealed,short," +
                    "sizeof,stackalloc,static,string,struct,switch,this,throw,true,try,typeof,uint,ulong,unchecked," +
                    "unsafe,ushort,using,virtual,void,volatile,while,var,dynamic,async,await"

            GLSL -> "attribute,break,const,continue,default,discard,do,else,for,if,in,inout," +
                    "layout,match,out,return,struct,uniform,while," +
                    "bool,int,uint,float,double,vec2,vec3,vec4,ivec2,ivec3,ivec4,uvec2,uvec3,uvec4,bvec2,bvec3,bvec4," +
                    "mat2,mat3,mat4,sampler1D,sampler2D,sampler3D,samplerCube,void"

            HLSL -> "bool,break,buffer,case,cbuffer,centroid,continue,default,discard,do,double,else,enum,export,extern," +
                    "false,float,for,groupshared,if,in,inout,int,line,lineadj,linear,matrix,noperspective,out,packoffset," +
                    "pass,pixelfragment,point,pointstream,return,register,row_major,sample,sampler,sampler1D,sampler2D," +
                    "sampler3D,samplerCUBE,shared,struct,technique,texture1D,texture2D,texture3D,textureCUBE,true,typedef," +
                    "uint,uniform,unorm,vector,void,while"

            GO -> "break,case,chan,const,continue,default,defer,else,fallthrough,for,func,go,goto,if,import," +
                    "interface,map,package,range,return,select,struct,switch,type,var"

            JAVASCRIPT -> "abstract,arguments,async,await,boolean,break,byte,case,catch,char,class,const," +
                    "continue,debugger,default,delete,do,double,else,enum,eval,export,extends,false,final," +
                    "finally,float,for,function,goto,if,implements,import,in,instanceof,int,interface,let," +
                    "long,native,new,null,package,private,protected,public,return,short,static,super,switch," +
                    "synchronized,this,throw,throws,transient,true,try,typeof,using,var,void,volatile,while,with,yield"

            JSON -> "null,true,false"

            KOTLIN -> "abstract,actual,annotation,as,break,by,catch,class,companion,const,constructor,continue," +
                    "crossinline,data,do,dynamic,else,enum,expect,external,false,final,finally,for,fun,get,if,import," +
                    "in,infix,init,inline,inner,interface,internal,is,it,lateinit,noinline,null,object,open,operator," +
                    "out,override,package,private,protected,public,reified,return,sealed,set,super,suspend,this," +
                    "throw,true,try,typealias,val,var,vararg,when,where,while,yield"

            PHP -> "__halt_compiler(,abstract,and,array(,as," +
                    "break,callable,case,catch,class," +
                    "clone,const,continue,declare,default," +
                    "die(,do,echo,else,elseif," +
                    "empty(),enddeclare,endfor,endforeach,endif," +
                    "endswitch,endwhile,eval(,exit(,extends," +
                    "final,finally,fn,for,foreach," +
                    "function,global,goto,if,implements," +
                    "include,include_once,instanceof,insteadof,interface," +
                    "isset(,list(,match,namespace,new," +
                    "or,print,private,protected,public," +
                    "readonly,require,require_once,return,static," +
                    "switch,throw,trait,try,unset(," +
                    "use,var,while,xor,yield," +
                    "yield from," +
                    "__CLASS__,__DIR__,__FILE__,__FUNCTION__,__LINE__," +
                    "__METHOD__,__PROPERTY__,__NAMESPACE__"

            PYTHON -> "False,True,None,and,as,assert,async,await,break,class,continue,def,del,elif,else," +
                    "except,finally,for,from,global,if,import,in,is,lambda,nonlocal,not,or,pass,raise," +
                    "return,try,while,with,yield"

            RUST -> "as,async,await,break,const,continue,crate,else,enum,extern,false,fn,for,if,impl,in,let,loop,match," +
                    "mod,move,mut,pub,ref,return,self,Self,static,struct,super,trait,true,type,unsafe,use,where,while," +
                    "dyn,abstract,become,box,do,final,macro,override,try,typeof,unsized,virtual,yield"

            SWIFT -> "associatedtype,as,break,case,catch,class,continue,default,defer,deinit,do,dynamic,else,enum," +
                    "extension,false,fileprivate,for,func,get,guard,if,import,in,inout,internal,is,let,mutating," +
                    "nil,nonmutating,open,operator,optional,override,postfix,precedence,prefix,private,protocol,public," +
                    "repeat,required,return,self,set,static,struct,subscript,super,switch,throw,throws,true,try,typealias," +
                    "unowned,var,weak,where,while,yield"

            ZIG -> "abort,align,asm,async,await,break,catch,comptime,const,continue,defer,else,enum,error," +
                    "export,extern,false,fn,if,import,in,inline,linksection,opaque,or,panic,packed,return,suspend," +
                    "switch,true,struct,undefined,union,var,volatile,while"
        }.splitKeywords(false)
    }

    val supportsTriangleStrings: Boolean
        get() = this == KOTLIN || this == PYTHON || this == SWIFT || this == ZIG

    val supportsBacktickStrings: Boolean
        get() = this == JAVASCRIPT

    val supportsDoubleSlashComment: Boolean
        get() = this != PYTHON

    val supportsHashTagComment: Boolean
        get() = this == PYTHON

    val supportsPreprocessor: Boolean
        get() = this == C || this == CPP || this == C_OR_CPP || this == CSHARP || this == GLSL || this == HLSL
}

#import <UIKit/UIKit.h>
#import <GLKit/GLKit.h>
#import <GameMain/GameMain.h>
@interface ViewController : GLKViewController
@property(strong, nonatomic) EAGLContext *context;
@property(strong, nonatomic) GameMainMyIosGameWindow2 *gameWindow2;
@property(strong, nonatomic) GameMainRootGameMain *rootGameMain;
@property(strong, nonatomic) NSMutableArray<NSNumber*> *freeIds;
@property(strong, nonatomic) NSMutableArray<NSNumber*> *touchesIds;
@property(strong, nonatomic) NSMutableArray<UITouch*> *touches;
@property() int lastTouchId;
@property boolean_t initialized;
@property boolean_t reshape;
@end
